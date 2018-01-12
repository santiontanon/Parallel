#!python

""" Tempest - Flexible temperature-map/heat-map generator

Evan Kaufman
Version 2010.09.26
Released under the MIT license <http://www.opensource.org/licenses/mit-license.php>

The goal of Tempest is to provide a consistent, reliable, multi-language API
for generating heat-map images based on coordinate data-sets.
"""

import os
import os.path

class Callable:
    def __init__(self, anycallable):
        self.__call__ = anycallable

class Tempest:
    
    """ Exposes the Tempest API through class instantiation.
    
    from Tempest import Tempest
    
    # Create new instance
    heatmap = Tempest(
      input_file = 'screenshot.png',
      output_file = 'heatmap.png',
      coordinates = [ [0,10], [2,14], [2,14] ]
    )
    
    # Configure as needed
    heatmap.set_image_lib( Tempest.LIB_PIL )
    
    # Generate and write heatmap image
    heatmap.render()
    """
    
    __version__ = '2010.09.26'
    
    _api_version = '2009.07.15'
    
    _path = os.path.abspath( os.path.dirname(__file__) )
    
    LIB_PIL = {'name':'PIL.Image'}
    """Forces use of PIL (currently the only supported lib)."""
    
    input_file = None
    """Image from which heatmap will be sized and over which it will be rendered"""
    
    output_file = None
    """Filename to which heatmap will be written, replacing any existing file without warning."""
    
    coordinates = None
    """Sequence of x,y coordinate pairs that will mark the center of all plotted data points."""
    
    plot_file  = _path + '/data/dot10.png'
    #plot_file  = _path + '/data/plot.png'
    """Image, presumably greyscale, used to plot data points.  Defaults to a bundled image, if none is provided."""
    
    color_file = _path + '/data/clut.png'
    color_file = _path + '/data/pgaitch.png'
    """Image, presumably a vertical gradient, used as a color lookup table.  Defaults to a bundled image, if none is provided."""
    
    overlay    = True
    """Boolean that indicates whether heatmap should be overlaid onto input image.  Defaults to True."""
    
    opacity    = 50
    """Percentage of opacity for overlay, from 0 (transparent) to 100 (opaque).  Defaults to 50."""
    
    image_lib  = None
    """Constant that indicates supported image library to use.  Defaults to PIL (currently the only lib available)."""
    
    def __init__(self, **kw):
        """Creates a new Tempest instance.
        
        Accepts named arguments of any class attributes, essentially
        accomplishing the same thing as calling their respective setters.
        
        The following arguments are required, all others are optional:
        - input_file
        - output_file
        - coordinates
        """
        
        # determine image library to use, if none given
        if not kw.has_key('image_lib'):
            kw['image_lib'] = self._calc_image_lib()
        
        # for all required parameters..
        for param_name in ('input_file', 'output_file', 'coordinates'):
            # ..call their respective setters if they're provided
            if kw.has_key(param_name):
                Tempest.__dict__['set_' + param_name]( self, kw[param_name] )
            # ..otherwise raise error
            else:
                pass
                ###raise TypeError('Missing required parameter \'%s\'' % param_name)
        
        # for all optional params..
        for param_name in ('plot_file', 'color_file', 'overlay', 'opacity', 'image_lib'):
            # ..for any provided, call their respective setters
            if kw.has_key(param_name):
                Tempest.__dict__['set_' + param_name]( self, kw[param_name] )
    
    def render(self):
        """Generates a heatmap image and writes it to the filesystem."""
        if self.image_lib == self.LIB_PIL:
            import Tempest.pil
            return Tempest.pil.render(self)
        else:
            raise ImportError('Image library \'%s\' is not supported' % image_lib['name'])
    
    def version():
        """Returns the version number of this implementation."""
        return Tempest.__version__
    version = Callable(version)
    
    def api_version():
        """Returns the version number of the currently supported Tempest API."""
        return Tempest._api_version
    api_version = Callable(api_version)
    
    def has_image_lib(image_lib):
        """Indicates whether the given image library is currently available for use."""
        if image_lib == Tempest.LIB_PIL:
            try:
                __import__(**image_lib)
                return True
            except ImportError:
                return False
        else:
            raise ImportError('Image library \'%s\' is not supported' % image_lib)
    has_image_lib = Callable(has_image_lib)
    
    def get_input_file(self):
        return self.input_file
    
    def set_input_file(self, input_file):
        if os.access(input_file, os.F_OK | os.R_OK):
            self.input_file = input_file
        else:
            raise Exception('Image \'%s\' is not readable' % input_file)
        return self
    
    
    def get_output_file(self):
        return self.output_file
    
    def set_output_file(self, output_file):
        if (not os.path.exists(output_file)) or os.access(output_file, os.F_OK | os.W_OK):
            self.output_file = output_file
        else:
            raise Exception('Image \'%s\' is not writable' % output_file)
        return self
    
    
    def get_coordinates(self):
        return self.coordinates
    
    def set_coordinates(self, coordinates):
        # verify an array of 2-element arrays
        if not hasattr(coordinates, '__iter__'):
            raise Exception('Bad coordinates: not an iterable type')
        
        for pair in coordinates:
            if not hasattr(pair, '__iter__') or len(pair) != 2:
                raise Exception('Bad coordinate pair: %s' % str(pair))
        
        self.coordinates = coordinates
        return self
    
    
    def get_plot_file(self):
        return self.plot_file
    
    def set_plot_file(self, plot_file):
        if os.access(plot_file, os.F_OK | os.R_OK):
            self.plot_file = plot_file
        else:
            raise Exception('Image \'%s\' is not readable' % plot_file)
        return self
    
    
    def get_color_file(self):
        return self.color_file
    
    def set_color_file(self, color_file):
        if os.access(color_file, os.F_OK | os.R_OK):
            self.color_file = color_file
        else:
            raise Exception('Image \'%s\' is not readable' % color_file)
        return self
    
    
    def get_overlay(self):
        return self.overlay
    
    def set_overlay(self, overlay):
        self.overlay = bool(overlay)
        return self
    
    
    def get_opacity(self):
        return self.opacity
    
    def set_opacity(self, opacity):
        opacity = int(opacity)
        if opacity >= 0 and opacity <= 100:
            self.opacity = opacity
        else:
            raise Exception('\'%s\' is not a valid percentage (integer from 0 to 100)' % opacity)
        return self
    
    
    def get_image_lib(self):
        return self.image_lib
    
    def set_image_lib(self, image_lib):
        if self.has_image_lib(image_lib):
            self.image_lib = image_lib
        else:
            raise ImportError('Image library \'%s\' could not be found' % image_lib['name'])
        return self
    
    
    def _calc_image_lib(self):
        for image_lib in (self.LIB_PIL,):
            if self.has_image_lib(image_lib):
                return image_lib
        raise ImportError('No supported image library could be found')
