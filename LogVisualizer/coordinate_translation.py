#!/bin/env python
import os
import ast
import math
import sys

CANVAS_W = 640
CANVAS_H = 360
EPSILON = 0.000000000000000001

def transform_log_file(path,fname,levelData):
    f = open(path + fname,'r+')
    data = [ d.strip("\n") for d in f.readlines() ]
    startTime_ = 0
    endTime_ = 0
    startParse = False

    # Used for converting from screen space to world space
    calib = (0,0)
    board_width = 0
    board_height = 0
    world_width = 0
    world_height = 0
    center_coord = ( )
    conversionComputed = False;
    currentLevel = ""
    direction_board = []
    color_board = []
    comp_color_map = {}
    comp_xy_map = {}

    for i in range(0,len(data)):
        date_, name_, data_, time_, x_, y_ = data[i].split("\t")
        time_  = float(time_)

        if name_ == "Calibration":
            print "Calibration"
            tmp = data_.split("x")
            calib = (int(tmp[0]),int(tmp[1]))
            startParse = True
            continue;

        if startParse:      
            # Normalize x,y coordinates 
            if conversionComputed:
                norm_x = (float(x_)/calib[0])
                norm_y = (float(y_)/calib[1])
                #xn = int(math.ceil(world_width*norm_x))
                #yn = int(math.ceil(world_height*norm_y))
                #print "World space coordinates: {0},{1}".format(xn,yn)

                # Translate xmin and ymin to (0,0)
                xmin = 0
                xmax = int(math.ceil( (center_coord[0] + float(board_width)/2.0) - (center_coord[0] - float(board_width)/2.0) ) )
                ymin = 0 
                ymax = int(math.ceil( (center_coord[1] + float(board_height)/2.0) - (center_coord[1] - float(board_height)/2.0) ) )

                #xmin = int(math.ceil(center_coord[0] - float(board_width)/2.0))
                #xmax = int(math.ceil(center_coord[0] + float(board_width)/2.0))
                #ymin = int(math.ceil(center_coord[1] - float(board_height)/2.0))
                #ymax = int(math.ceil(center_coord[1] + float(board_height)/2.0))

                xn = int(math.ceil( (world_width*norm_x) - (center_coord[0] - float(board_width)/2.0) ))
                yn = board_height - int(math.ceil( (world_height*norm_y) - (center_coord[1] - float(board_height)/2.0))) # top of board is 0, but bottom is 0 in Unity 

                print "World space coordinates: {0},{1}".format(xn,yn)

                # If we are within this grid, then we're on the board...
                if xn >= xmin and xn <= xmax and yn >= ymin and yn <= ymax:
                    # Translate xmin and ymin to (0,0)
                    #pass
                    if ( xn != 0 ):
                        xn -= 1 
                    if ( yn != 0 ): 
                        yn -= 1
                    #print xn, ",", yn , "," , color_board[yn]
                    if name_ == "OnMouseComponent":
                        tmp = data_.split("/")
                        if tmp[1] in comp_xy_map.keys():
                            print "Actual coordinates: ", comp_xy_map[tmp[1]]
                    print "Color of item on the board: {0}".format(color_board[yn][xn])
                    #print "We're on the board..{0} {1}".format(data[i],currentLevel)
                    print "xmin,xmax,ymin,ymax: {0},{1},{2},{3}".format(xmin,xmax,ymin,ymax)
                else:
                    #print "World space coordinates: {0},{1}".format(xn,yn)
                    #print "We're not on the board..{0} {1}".format(data[i],currentLevel)
                    #print "xmin,xmax,ymin,ymax: {0},{1},{2},{3}".format(xmin,xmax,ymin,ymax)

            #print "Normalized coordinates: ", (float(x_)/calib[0])*wlevel, "," , (float(y_)/calib[1])*hlevel

            if name_ == "TriggerLoadLevel": 
                if data_:
                    if data_.startswith('l'):
                        currentLevel = data_
                        # Don't consider this...yet..
                        conversionComputed = False
                    else:
                        currentLevel = data_
                        if len(direction_board) != 0:
                            direction_board = []
                        if len(color_board) != 0:
                            color_board = [] 
                        if len(comp_color_map) != 0:
                            comp_color_map = {}
                        if len(comp_xy_map) != 0:
                            comp_xy_map = {}

                        if data_ in levelData:
                            conversionComputed = False
                            dfile = open(path + '/data' + "/" + levelData[data_][0],'r')
                            dfiledata = [ d.strip("\n") for d in dfile.readlines() ]
                            startDataParse = False
                            height = 0
                            width = 0
                            incr = 0
                            for line in dfiledata:
                                if line.startswith("board_width"):
                                    width = int(line.split("\t")[1])
                                    board_width = int(line.split("\t")[1])
                                if line.startswith("board_height"):
                                    height = int(line.split("\t")[1])
                                    board_height = int(line.split("\t")[1])
                                if line.startswith('COLOR'):
                                    for i in range(incr+1,incr+board_height+1):
                                        color_board.append(dfiledata[i])
                                if line.startswith('DIRECTIONS'):
                                    for i in range(incr+1,incr+board_height+1):
                                        direction_board.append(dfiledata[i])

                                if line.startswith('COMPONENTS'): 
                                    startDataParse = True 
                                else:
                                    if startDataParse:
                                        if len(line) == 0: # End of components section
                                            startDataParse = False 
                                        else:
                                            tmp = line.split("\t")
                                            compData = ast.literal_eval(tmp[6])
                                            if "color" in compData.keys():
                                                # CompID -> color
                                                comp_color_map[tmp[0]] = int(compData["color"])  
                                            comp_xy_map[tmp[0]] = (int(tmp[2]),int(tmp[3]))
                                            #if "link" in compData.keys():                             
                                            #    # CompID -> LinkedID
                                            #    compLinkMap[tmp[0]] = str(compData["link"])

                                incr += 1
                            print "Width, height: ", width, ",", height
                            aspectRatio = float(calib[0])/float(calib[1]) # Aspect Ratio = Screen Width / Screen Height
                            levelRatio = float(width)/float(height) 

                            print "Aspect Ratio, Level Ratio: " , aspectRatio, ",", levelRatio
                            if levelRatio > 1.78: # 16:9
                                height *= (levelRatio/1.78 ) 
                                height /= 0.9;
                            else:
                                height += 1
                                 
                            world_height = (float(height)/0.78)
                            world_width = aspectRatio*float(world_height) 
                            print "World height,width: {0},{1} ".format(world_height,world_width)
                            center_coord = ( world_width/2.0, world_height/2.0 )
                            print "Center coordinate: {0}".format(center_coord)
                            conversionComputed = True

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
    
    #if len(sys.argv) < 3:
    #    print "Usage: python coordinate_translation.py"
    #    sys.exit(-1)
    
    PATH = "../35_saved_data"
    LOGPATH = PATH + "/log/"
    levelData = getLevelData(PATH)
    for f in os.listdir(LOGPATH):  
        print "File: {0}".format(f)
        transform_log_file(LOGPATH,f,levelData)
