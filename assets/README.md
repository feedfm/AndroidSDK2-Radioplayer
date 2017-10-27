
The mask files in 'masks' can be used to generate new icons
with a specific color. Make sure ImageMagick is installed
on your computer, then run the colorize.sh script like so:

  ./colorize.sh '#00ff00'

That will update the icons in the 'playeractivity/src/main/res/drawable'
directories to all be blue.

The masks were created from our source art with:

convert src.png -alpha extract mask.png

and colorize.sh runs the following to create the final images:

convert mask.png -background #color -alpha shape dest.png

There is probably a one-liner to do all this, but I don't know what
it is.

