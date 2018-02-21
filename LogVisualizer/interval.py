#!/bin/env python
import os
import sys
import ast
import math

def getDataInIntervals(data,t1,t2,ind):
    tmp = []
    for i in range(ind,len(data)):
        date_, name_, data_, time_, x_, y_ = data[i].split("\t")
        time_  = float(time_)
        if ( (t1 < time_) and (t2 > time_) ):
            tmp.append(data[i])  
            ind = i
    return (tmp,ind)

def readFromLogDirectory(log_path,level_data,interval):
    ret_data = { }
    for f in os.listdir(log_path):  
        f = open(log_path + f,'r+')
        data = [ d.strip("\n") for d in f.readlines() ]
        split_data = []    
        t1 = 0
        t2 = interval
        tmp = [-1]
        i = 0
        while ( i != len(data)-1 ):
            (tmp,i) = getDataInIntervals(data,t1,t2,i)
            if i == len(data)-1:
                break
            print "Time interval {0} to {1} with data: {2}".format(t1,t2,tmp)
            split_data.append(tmp)
            t1 += float(interval)/2.0 # We want to stagger the time windows
            t2 = t1 + interval
        ret_data[f] = split_data

def getLevelData(path):
    filesDict = {} 
    for i in os.listdir(path + '/data'):
        with open(path + '/data/' + i) as f:
            for line in f:
                if line.startswith('filename'):
                    filename = line.split('\t')[1].strip()
                    if filename in filesDict: 
                        print "Duplicates detected"
                        filesDict[filename].append(i) 
                    else:
                        filesDict[filename] = [i]
    return filesDict


if __name__ == "__main__": 
    if len(sys.argv) < 2:
        print "Usage: python interval.py interval"
        sys.exit(0)

    interval = int(sys.argv[1])
    PATH = "../35_saved_data"
    LOGPATH = PATH + "/log/"
    levelData = getLevelData(PATH)
    readFromLogDirectory(LOGPATH,levelData,interval)
