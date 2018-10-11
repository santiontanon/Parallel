import socket
from http.server import BaseHTTPRequestHandler, HTTPServer
import socketserver
import logging as log
import os
import argparse
import sys
from datetime import date, datetime
import uuid
from urllib.parse import urlparse, parse_qs

import log_analysis

#TODO: Add player model logging to the server

start_counter_id = 0

try:
    import simplejson as json
except ImportError:
    import json

PORT = 8787
ROOT_DATA_PATH = 'saved_data/'
START_COUNTER_DEFAULT = 3000

def initialize_data_directory():
    for directory in ['data','id','log', 'playermodel']:
        path = os.path.dirname(os.path.abspath(__file__))
        path = path + '/' + ROOT_DATA_PATH + directory
        try:
            log.debug("ROOT DATA PATH: {}".format(path))
            os.makedirs(path, exist_ok=True)
        except:
            raise OSError("Unable to create root data directory {}".format(ROOT_DATA_PATH))
            sys.exit(-1)

def save_data(data, directory = ''):
    path = os.path.dirname(os.path.abspath(__file__))
    path = path + '/' + ROOT_DATA_PATH + directory
    try:
        os.makedirs(path)
    except:
        pass
    while True:
        file_name = path + '/' + uuid.uuid4().hex
        if not os.path.isfile(file_name):
            break
    with open(file_name, 'w') as f:
        f.write(data)

def get_player_model_data(user_name):
    path = os.path.dirname(os.path.abspath(__file__))
    filename = "{}.json".format(user_name)
    path = path + '/' + ROOT_DATA_PATH + "/playermodel/" + filename
    log.debug("Filename: {}".format(path))
    if not os.path.isfile(path):
        return ""
    f = open(path, 'r')
    json_data = f.readlines()
    json_data = "\n".join(json_data)
    json_data = json.loads(json_data)
    log.debug("Data for user {}: {}".format(user_name, json_data))
    return json_data

class Handler(BaseHTTPRequestHandler):
    def log_request(s, message=""):
        log.debug("%s: %s/%s" %(s.client_address, s.command, s.path))

    def send_error_message(self, msg = "only accepting post of application/json"):
        self.send_response(200, "OK")
        self.send_header('Content-type', 'application/json')
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        json_str = json.dumps({"error": msg})
        self.wfile.write(str.encode(json_str))

    def send_ok_message(self):
        self.send_response(200, "OK")
        self.send_header('Content-type', 'application/json')
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        json_str = json.dumps({"status": "OK"})
        self.wfile.write(str.encode(json_str))

    def do_GET(self):
        self.log_request()
        if self.path.strip() == '/status':
            log_analysis.write_to_web_status_panel(self, ROOT_DATA_PATH)
        elif "/playermodel" in self.path.strip():
            query_components = parse_qs(urlparse(self.path).query)
            user_name = query_components["user"][0]
            log.debug("Query components: {}".format(query_components))
            # Find user in /playermodel directory and get data (empty if player doesn't exist)
            json_data = get_player_model_data(user_name)
            self.send_response(200, "OK")
            self.send_header('Content-type', 'application/json')
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            json_str = json.dumps(json_data)
            self.wfile.write(str.encode(json_str))
        else:
            self.send_error_message()

    def do_OPTIONS(self):
        self.log_request()
        self.send_response(200, "OK")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header("Access-Control-Allow-Headers", "content-type")
        self.send_header("Access-Control-Allow-Headers", "X-Requested-With")
        self.end_headers()
        self.wfile.write(str.encode("OK"))

    def do_POST(self):
        self.log_request()

        path = self.path
        content_type = self.headers['Content-type']

        log.debug("ajaxserver.POST: Path           %s" % path)
        log.debug("ajaxserver.POST: Content-type   %s" % content_type)

        content_length = int(self.headers['Content-length'])
        content = self.rfile.read(content_length)

        log.debug("ajaxserver.POST: Content-length %d" % content_length)
        log.debug("ajaxserver.POST: Content:       %s" % content)

        if path.strip() == '/id':
            if not content_type.startswith('application/json'):
                self.send_error_message()
                return

            try:
                data = json.loads(content)
            except:
                self.send_error_message("bad json")
                return
            if False and 'user' not in data:
                self.send_error_message("missing input parameter")
                return

            global start_counter_id
            start_counter_id += 1
            data['id'] = start_counter_id
            save_data(json.dumps(data),'/id')

            try:
                ret = json.load(open('LevelLoadSelection.txt'))
            except:
                ret = data
            ret['id'] = start_counter_id

            self.send_response(200, "OK")
            self.send_header('Content-type', 'application/json')
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            json_str = json.dumps(ret)
            self.wfile.write(str.encode(json_str))
            return
        elif path.strip() == '/log':
            save_data(content, '/log')
            self.send_ok_message()
            return
        elif path.strip() == '/data':
            save_data(content, '/data')
            self.send_ok_message()
            return
        elif path.strip() == "/playermodel":
            try:
                data = json.loads(content)
            except:
                self.send_error_message("bad json")
                return
            save_data(data, "/playermodel")
            self.send_ok_message()
            return
        else:
            self.send_error_message("wrong path")
            return

class ForkingHTTPServer(socketserver.ThreadingTCPServer, HTTPServer):
    def finish_request(self, request, client_address):
        request.settimeout(300)
        HTTPServer.finish_request(self, request, client_address)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-v", "--verbosity", type=int, help="increase output verbosity", default=1)
    parser.add_argument("-s", "--host", type=str, help="host name", default=None)
    parser.add_argument("-p", "--port", type=int, help="port number", default=PORT)
    parser.add_argument("-i", "--id", type=int, help="initial id", default=START_COUNTER_DEFAULT)
    args = parser.parse_args()
    port = args.port
    if args.host:
        host = args.host
    else:
        host = socket.gethostbyname(socket.gethostname())
    if args.verbosity > 0:
        log.basicConfig(level = log.DEBUG, format   = '%(asctime)s %(levelname)-8s %(message)s', datefmt  = '%a, %d %b %Y %H:%M:%S')

    log.info("------------------- Creating data directory... ------------------- ")
    initialize_data_directory()
    log.info("------------------- Created data directory! ------------------- ")

    global start_counter_id
    start_counter_id = args.id
    current_largest_id = log_analysis.get_largest_id(ROOT_DATA_PATH)
    start_counter_id = max(start_counter_id, current_largest_id)
    print("Starting counter id at {}".format(start_counter_id))

    httpd = None
    try:
        # start the server
        log.info("ajaxserver: starting HTTP server on %s:%d" %(host, port))
        server_address = (host, port)
        httpd = ForkingHTTPServer(server_address, Handler)
        log.info("ajaxserver: waiting for requests")
        httpd.serve_forever()
        log.info("ajaxserver: terminating")
    except KeyboardInterrupt:
        log.info("User has terminated AJAX Server!")
        if httpd: httpd.socket.close()

if __name__ == '__main__':
    main()
