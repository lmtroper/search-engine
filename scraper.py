import requests
from bs4 import BeautifulSoup
import gzip
from datetime import datetime
import validators
import threading
from multiprocessing import Value

NUM_ARTICLES = 2000
BASE_URL = 'https://www.cbc.ca/'
urls_to_visit = [
	"https://www.cbc.ca/news/science/district-heating-explainer-1.7113827",
	"https://www.cbc.ca/news/indigenous/b-c-land-act-dripa-1.7112974",
	"https://www.cbc.ca/news/world/us-russia-security-concern-canada-allies-1.7115633",
	"https://www.cbc.ca/news/canada/soulmate-research-1.7113582",
	"https://www.cbc.ca/news/business/rentals-report-average-asking-1.7114976",
	"https://www.cbc.ca/news/entertainment/alicia-keys-super-bowl-voice-crack-1.7114795"
]
dates_scraped = {}
visited_urls = {}

NUM_THREADS = 10

num_visited = Value('i', 0)
working_threads = Value('i', NUM_THREADS)

# Condition variable for waiting threads for new urls
waiting_threads_cond = threading.Condition() 

num_visited_lock = threading.Lock()
urls_to_visit_lock = threading.Lock()
visited_urls_lock = threading.Lock()
write_lock = threading.Lock()

def datetime_to_string(date):
	date = date["datetime"][:10].split('-')
	# Convert the input date components to a datetime object
	input_date_str = '-'.join(date)
	input_datetime = datetime.strptime(input_date_str, '%Y-%m-%d')

	# Desired output date format
	desired_output_format = "%B %d, %Y"

	# Format the input datetime according to the desired output format
	return input_datetime.strftime(desired_output_format)
	
def docno_generator(time_tag):
	time_tag = time_tag["datetime"][:10].split('-')
	return time_tag[1]+time_tag[2]+time_tag[0][2:]

def scrape(url):
	try:
		response = requests.get(
			url=url,
		)
		if response.status_code != 200:
			return False

		soup = BeautifulSoup(response.content, 'html.parser')

		# Get all links on the page and extend link list
		content_element = soup.find(id="content")
		if content_element is None:
			return False
		
		all_links = content_element.find_all("a")
		parsed_links = [link['href'] for link in all_links if BASE_URL in link['href']]
		with urls_to_visit_lock, waiting_threads_cond:
			urls_to_visit.extend(parsed_links)
			waiting_threads_cond.notify_all()

		# Get the article's title, subtitle, body, and time
		title = soup.find(class_="detailHeadline")
		subtitle = soup.find(class_="deck")
		body = soup.find('div', class_='story')
		time_tag = soup.find('time', class_='timeStamp')

		# Generate the article's docno
		docno, date = None, None
		if time_tag:
				docno = docno_generator(time_tag)
				date = datetime_to_string(time_tag)
		else:
				return False
		
		# Check for number of articles with the same date
		if docno in dates_scraped:
			dates_scraped[docno] += 1
		else:
			dates_scraped[docno] = 1
		length_date = len(str(dates_scraped[docno]))
		zeros = (4-length_date)*"0"
		docno = f"CB{docno}-{zeros}{dates_scraped[docno]}"

		# Check if the article has all the required fields
		if docno and date and title.text and subtitle.text and body:
			content = body.find_all(['p', 'h2'])
			# Ignore articles with less than 3 paragraphs
			if not content or len(content) <= 2:
				return False
			else:
				data = {
					"docno": docno,
					"date": date,
					"title": title.text.rstrip(),
					"subtitle": subtitle.text.rstrip(),
					"content": content,
				}
				
				write_to_file(data)
		return True
	except Exception as e:
		return False

def write_to_file(data):
	print('writing to file')
	with write_lock:
		with open("data.txt", "a+") as file:
			file.write("<DOC>\n")
			file.write(f"<DOCNO> {data['docno']} </DOCNO>\n")
			file.write(f"<DATE>\n<P>\n{data['date']}\n</P>\n</DATE>\n")
			file.write("<HEADLINE>\n")
			file.write(f"<P>\n{data['title']}; \n</P>\n")
			file.write(f"<P>\n{data['subtitle']}\n</P>\n")
			file.write("</HEADLINE>\n")
			file.write("<TEXT>\n")
			
			for element in data['content']:
				if element and len(element) > 0:
					file.write(f"<P>\n{element.get_text()}\n</P>\n")

			file.write("</TEXT>\n")
			file.write("</DOC>\n")

def worker(num_visited, working_threads):
	while True:
		if num_visited.value <= NUM_ARTICLES:
			if len(urls_to_visit) == 0:
				with waiting_threads_cond:
					working_threads.value -= 1
					if working_threads.value == 0:
						return
					waiting_threads_cond.wait()
			else:
				with urls_to_visit_lock:
					curr_url = urls_to_visit[0]
					urls_to_visit.pop(0)
				if curr_url in visited_urls:
					continue
				else:
					with visited_urls_lock:
						visited_urls[curr_url] = True
					# Ignore links that don't start with the base URL or are not articles
					if curr_url.startswith(BASE_URL) and validators.url(curr_url) and len(curr_url.split('/')) >= 6:
						res = scrape(curr_url)
						if res:
							with num_visited_lock:
								num_visited.value += 1
								print('scraping', curr_url)
								print(num_visited.value)
		else:
			return
		
# with open("data.txt", "w") as file:
# 	scrape(urls_to_visit[0], file=file)
# 	urls_to_visit.pop(0)

threads = []
for _ in range(NUM_THREADS):  # Number of worker threads
	thread = threading.Thread(target=worker, args=(num_visited,working_threads))
	thread.start()
	threads.append(thread)

# Wait for all threads to finish
for thread in threads:
	thread.join()

with open("data.txt", "rb") as file_in:
    with gzip.open("data.gz", "wb") as file_out:
        file_out.writelines(file_in)

print("Data has been written to data.txt and gzipped to data.txt.gz")
