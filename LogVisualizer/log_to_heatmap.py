# Note, this needs PIL; I'm using Pillow==3.4.2 instead.
import sys, os
import collections,operator
#from euclid import Point3,Point2
from Tempest import Tempest
#import matplotlib.pyplot as plt

from PIL import Image
import PIL as xxx

EVENT_NAME = 1
EVENT_MOUSE_X = 4
EVENT_MOUSE_Y = 5
SIZE_W = 1600.0
SIZE_H = 900.0
CANVAS_W = 640
CANVAS_H = 360
EVENTS = ['MouseMoveD','MouseMoveV','MouseMoveS','MouseDown','MouseUp']
EVENTS = ['MouseDown']


def fix_x(s):
    return int(float(s)/SIZE_W*CANVAS_W)

def fix_y(s):
    return int(float(s)/SIZE_H*CANVAS_H)

def heatmap(f):
    print "processing "+f
    data = [i.split('\t') for i in open(f).readlines()]
    coordinates = [(fix_x(i[EVENT_MOUSE_X]),fix_y(i[EVENT_MOUSE_Y])) for i in data if i[EVENT_NAME] in EVENTS]
    if len(coordinates) > 0:
        heatmap = Tempest(
          input_file = 'canvas.png',
          coordinates = coordinates,
        )
        img = heatmap.render()
        open(f+'.click.png','wb').write(img)
path = '/Users/josepvalls/paraprog/data/traces/'
for root, dirs, files in os.walk(path):
    for i in files:
        if i.endswith('.log'):
            heatmap(root+'/'+i)