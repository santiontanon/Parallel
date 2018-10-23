import os
import sys
import json
import jsonpickle
import datetime
import argparse
import collections
import util
import math
import ast
import datetime

epoch = datetime.datetime.utcfromtimestamp(0)

def get_largest_id(ROOT_DATA_PATH):
    highest_id_value = 0
    root_path = os.path.dirname(os.path.abspath(__file__)) + '/' + ROOT_DATA_PATH
    for file_ in os.listdir(root_path + '/id'):
        # Three keys: version, user, id
        data = json.load(open(root_path + '/id/' + file_))
        try:
            id_ = int(data['id'])
            if id_ > largest_id:
                highest_id_value = id_
        except:
            continue
    for file_ in os.listdir(root_path + '/log'):
        data = get_file_data(root_path + '/log/' + file_, dict())
        if 'id' in data and data['id'].isdigit():
            data['id'] = int(data['id'])
            if data['id'] > highest_id_value:
                highest_id_value = data['id']
    return highest_id_value

def html_table_from_list_of_dicts(table, labels=None, options={}):
    separator = '\n'
    table_data = html_wrap('tr', separator.join([html_wrap('th', i) for i in labels]))
    table_data += separator.join([html_wrap('tr',
                                            separator.join([html_wrap('td', row.get(i, '')) for i in labels]))
                                     for row in table])
    return html_wrap('table border=1', table_data)

def html_wrap(tag, contents, extra_html=None):
    return '<%s>%s</%s>' % (tag, contents, tag.split()[0])

def write_to_web_status_panel(writer, ROOT_DATA_PATH):
    '''This is used for the web status panel'''
    columns = 'id,num,user,version,start,end,len,levels,me,uploaded,filename,seq'.split(',')
    root_path = os.path.dirname(os.path.abspath(__file__)) + '/' + ROOT_DATA_PATH
    web_status_data = {}
    me_output_files = {}
    for file_ in os.listdir(root_path + '/data'):
        with open(root_path + '/data/' + file_) as f:
            for line in f:
                if line.startswith('filename'):
                    # Get filename of data with ME output.
                    filename = line.split('\t')[1].strip()
                    if filename in me_output_files:
                        me_output_files[filename] += 1
                    else:
                        me_output_files[filename] = 0
    for file_ in os.listdir(root_path + '/id'):
        # Three keys: version, user, id {"version": "Study3/RC2", "user": "uname", "id": num}
        data = json.load(open(root_path + '/id/' + file_))
        if data['id'] in web_status_data:
            data['num'] = web_status_data[data['id']]['num']+1
        else:
            data['num'] = 1
        web_status_data[data['id']] = data

    for file_ in os.listdir(root_path + '/log'):
        data = get_file_data(root_path + '/log/' + file_, me_output_files)
        data['filename'] = file_
        data['seq'] = ' '.join(data['seq'])
        #if 'id' in data and data['id'] in data_dict and data['user'] == data_dict[data['id']]['user']:
        if 'id' in data:
            if data['id'] in web_status_data:
                web_status_data[data['id']].update(data)
            else:
                web_status_data[data['id']] = data
        else:
            print("no match: {}".format(data))

    # Write to web server panel
    data = sorted(web_status_data.values(), key=lambda i:i['id'])
    table = html_table_from_list_of_dicts(data, columns)
    writer.send_response(200, "OK")
    writer.send_header('Content-type', 'text/html')
    writer.send_header("Access-Control-Allow-Origin", "*")
    writer.end_headers()
    html_str = html_wrap('html',html_wrap('body', table))
    writer.wfile.write(str.encode(html_str))


def get_file_data(fname, me_execution_files):

    data = {'levels': 0, 'me': 0, 'uploaded': 0, 'seq': list()}
    last_line = ""
    start_time = None
    with open(fname) as f:
        for line in f:
            try:
                t_, e_, d_, s__, x_, y_ = line.split('\t')
                s_ = datetime.datetime.strptime(t_,'%m-%d-%y-%H-%M-%S')
                s_ = (s_ - epoch).total_seconds()
                if not start_time:
                    start_time = s_
                    s_ = 0.0
                else:
                    s_ = s_ - start_time

                if 'start' not in data:
                    data['start'] = t_
                if e_=='SessionID' and 'id' not in data:
                    data['id'] = d_
                if e_=='SessionUser' and 'user' not in data:
                    data['user'] = d_
                if e_=='TriggerLoadLevel':
                    if d_:
                        if d_.startswith('l'):
                            data['levels'] += 1
                        else:
                            data['me'] += 1
                            if d_ in me_execution_files:
                                data['uploaded'] += 1
                    else:
                        data['seq'].append('R')
                elif e_=='SubmitCurrentLevelPlay':
                    data['seq'].append('T')
                elif e_=='SubmitCurrentLevelME':
                    data['seq'].append('S')

                last_line = line
            except:
                print(sys.exc_info()[0])
                raise
                #continue
    if last_line:
        data['end'] = last_line.split('\t')[0]
        try:
            data['len'] = "%.1f" % (float(last_line.split('\t')[3]))
        except:
            data['len'] = '?'
    return data
