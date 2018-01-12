import socket
import BaseHTTPServer
import SocketServer
import logging as log
import os
import argparse
import sys
import urllib2
from datetime import date,datetime
import uuid

import log_analysis


start_counter_id = 0

try:
    import simplejson as json
except ImportError:
    import json

PORT = 8787
DATA_PATH = 'saved_data'
START_COUNTER_DEFAULT = 3000

def init_saved_data_dirs():
    for i in ['data','id','log']:
        path = os.path.dirname(os.path.abspath(__file__))
        path = path + '/' + DATA_PATH +'/'+ i
        try:
            os.makedirs(path)
        except:
            pass

def save_data(data, extra_path = ''):
    path = os.path.dirname(os.path.abspath(__file__))
    path = path + '/' + DATA_PATH + extra_path
    try:
        os.makedirs(path)
    except:
        pass
    while True:
        file_name = path + '/' + uuid.uuid4().hex
        if not os.path.isfile(file_name):
            break
    with open(file_name,'w') as f:
        f.write(data)



class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
    def logRequest(s):
        log.debug("%s: %s/%s" %(s.client_address, s.command, s.path))

    def send_error_message(self, msg = "only accepting post of application/json"):
        self.send_response(200, "OK")
        self.send_header('Content-type', 'application/json')
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps({"error": msg}))

    def send_ok_message(self):
        self.send_response(200, "OK")
        self.send_header('Content-type', 'application/json')
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "OK"}))

    def do_GET(self):
        self.logRequest()
        if self.path.strip() == '/status':
            log_analysis.get_file_status(self)
        else:
            self.send_error_message()

    def do_OPTIONS(self):
        self.logRequest()
        self.send_response(200, "OK")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header("Access-Control-Allow-Headers", "content-type")
        self.send_header("Access-Control-Allow-Headers", "X-Requested-With")
        self.end_headers()
        self.wfile.write("OK")
  
    def do_POST(self):
        self.logRequest()

        path = self.path
        contentType = self.headers['Content-type']

        log.debug("ajaxserver.POST: Path           %s" % path)
        log.debug("ajaxserver.POST: Content-type   %s" % contentType)

        contentLength = int(self.headers['Content-length'])
        content = self.rfile.read(contentLength)

        log.debug("ajaxserver.POST: Content-length %d" % contentLength)
        log.debug("ajaxserver.POST: Content:       %s" % content)

        if path.strip() == '/id':
            if not contentType.startswith('application/json'):
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
            self.wfile.write(json.dumps(ret))
            return
        elif path.strip() == '/log':
            save_data(content, '/log')
            self.send_ok_message()
            return
        elif path.strip() == '/data':
            save_data(content, '/data')
            self.send_ok_message()
            return

        else:
            self.send_error_message("wrong path")
            return

class ForkingHTTPServerOld(SocketServer.ForkingMixIn, BaseHTTPServer.HTTPServer):
    def finish_request(self, request, client_address):
        request.settimeout(30)
        BaseHTTPServer.HTTPServer.finish_request(self, request, client_address)

class ForkingHTTPServer(SocketServer.ThreadingTCPServer, BaseHTTPServer.HTTPServer):
    def finish_request(self, request, client_address):
        request.settimeout(300)
        BaseHTTPServer.HTTPServer.finish_request(self, request, client_address)




def main():
    init_saved_data_dirs()
    try:
        host = socket.gethostbyname(socket.gethostname())
    except:
        host = '0.0.0.0'

    parser = argparse.ArgumentParser()
    parser.add_argument("-v", "--verbosity", type=int, help="increase output verbosity", default=1)
    parser.add_argument("-s", "--host", type=str, help="host name", default=None)
    parser.add_argument("-p", "--port", type=int, help="port number", default=PORT)
    parser.add_argument("-i", "--id", type=int, help="initial id", default=START_COUNTER_DEFAULT)
    args = parser.parse_args()

    port = args.port
    global start_counter_id
    start_counter_id = args.id
    largest_id = log_analysis.get_file_status(None)
    if largest_id > start_counter_id:
        start_counter_id = largest_id
    print "starting counter at %d" % start_counter_id

    if args.verbosity > 0:
        log.basicConfig(level    = log.DEBUG, format   = '%(asctime)s %(levelname)-8s %(message)s', datefmt  = '%a, %d %b %Y %H:%M:%S')
    if args.host:
        host = args.host

    httpd = None
    try:
        # start the server
        log.info("ajaxserver: starting HTTP server on %s:%d" %(host, port))
        server_address = (host, port)
        #httpd = BaseHTTPServer.HTTPServer(server_address, Handler)
        httpd = ForkingHTTPServer(server_address, Handler)
        log.info("ajaxserver: waiting for requests")
        httpd.serve_forever()
        log.info("ajaxserver: terminating")
    except KeyboardInterrupt, k:
        print "\rterminated by user, good bye!"
        log.info("aborted by user, terminating")
        if httpd: httpd.socket.close()

if __name__ == '__main__':
    main()