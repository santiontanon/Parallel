#!python

"""PIL adapter.

Implements Tempest image operations using the Python Imaging Library:
http://www.pythonware.com/products/pil/
"""

import PIL.Image as Image
import PIL.ImageDraw as ImageDraw
import PIL.ImageChops as ImageChops
import StringIO
def render(parent):
    input_file,output_file=None,None
    if(parent.get_input_file()):
        # load source image (in order to get dimensions)
        input_file = Image.open(parent.get_input_file())
    
        # create object for destination image
        output_file = Image.new('RGBA', input_file.size, 'white');
    else:
        output_file = Image.new('RGBA', (500,500), 'white');
    
    # do any necessary preprocessing & transformation of the coordinates
    coordinates = parent.get_coordinates()
    max_rep = 0
    normal = {}
    
    for pair in coordinates:
        # normalize repeated coordinate pairs
        pair_key = '%sx%s' % pair
        if normal.has_key(pair_key):
            normal[pair_key][2] += 1
        else:
            normal[pair_key] = [ pair[0], pair[1], 1 ]
        # get the max repitition count of any single coord set in the data
        if normal[pair_key][2] > max_rep:
            max_rep = normal[pair_key][2]
    
    coordinates = normal.values()
    normal = None
    
    # load plot image (presumably greyscale)
    plot_file = Image.open(parent.get_plot_file())
    
    # calculate coord correction based on plot image size
    plot_correct = [ (plot_file.size[0] / 2), (plot_file.size[1] / 2) ]
    
    # colorize opacity for how many times at most a point will be repeated
    colorize_percent = float(99 / max_rep);
    if colorize_percent < 1:
        colorize_percent = 1
    plot_file = colorize( plot_file, colorize_percent )
    
    # define a mask image for multiply operations
    white_mask = Image.new('RGBA', output_file.size, 'white')
    
    # paste one plot for each coordinate pair
    for pair in coordinates:
        x = pair[0] - plot_correct[0]
        y = pair[1] - plot_correct[1]
        
        # for how many times coord pair was repeated
        for i in range(0, pair[2]):
            # add to white mask centered on given coords
            point_mask = white_mask.copy()
            point_mask.paste(plot_file, (x, y))
            # multiply into output image
            output_file = ImageChops.multiply(output_file, point_mask)
    
    # open given spectrum and resize to 256px height
    color_file = Image.open(parent.get_color_file())
    
    # resizing lookup table, to simplify colorization
    color_file = color_file.resize( (color_file.size[0], 256,) )
    
    # get pixel access object (significantly quicker than getpixel method)
    color_pix = color_file.load()
    
    # apply color lookup table
    # note: optimized solution here courtesy of Nadia by way of Stackoverflow
    pix_data = output_file.getdata()
    output_file.putdata([color_pix[0, red] for (red, green, blue, alpha) in pix_data])
    
    # overlay heatmap over source image
    if parent.get_overlay() == True:
        output_file.putalpha( (256 * parent.get_opacity()) / 100 )
        if input_file:
            input_file.paste(output_file, None, output_file)
            output_file = input_file
    
    if False:
        coordinates = parent.get_coordinates()
        draw = ImageDraw.Draw(output_file) 
        pair0 = coordinates[0]
        for i,pair in enumerate(coordinates[1:]):
            draw.line((pair0[0],pair0[1],pair[0],pair[1]), fill=(255*i/len(coordinates),0,255), width=3)
            pair0 = pair

    
    # write destination image
    io = StringIO.StringIO()
    output_file.save(io,format="png")
    contents = io.getvalue()
    io.close()
    
    # return true if successful
    return contents

def colorize(image_file, opacity):
    # Image.blend(image1, image2, alpha) => image
    # 0.0 = image1, 1.0 = image2
    
    # blend with plain white
    white_point = Image.new('RGB', image_file.size, 'white')
    return Image.blend(image_file, white_point, ((100 - opacity) / 100) )
