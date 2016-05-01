# timestable

Application cycling throgh times tables and rendering them on a circle

n points are distributed evenly on the edge of a circle, numbered 0..n-1
Each point k is multiplied by a factor (mod n), resulting in a number x. Now a
line is drawn between point k and the point representing the number x.

The factor is being changed between renders, resulting in an animation.

## Usage

    $ java -jar timestable-0.1.0-standalone.jar [args]

or

    $ lein run
