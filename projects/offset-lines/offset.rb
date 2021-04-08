require 'clipper'
require 'fast_noise'
require 'mustache'
require 'optparse'
require 'ostruct'
require 'pry'
require 'pry-byebug'
require 'set'

NUM_RUNS = 1
DEFAULT_OUTPUT_PATH = '~/Genart/sketchout/offsetstripes/svg'

$num_runs = NUM_RUNS
$out_path = DEFAULT_OUTPUT_PATH
$width = 480
$height = 680
$seed = rand(2**31)
$yoff = 0

def set_globals
  $stripe_width = rand(3..9)
  $stripe_offset = rand(0.3..$stripe_width.to_f)
  #560x794

  $noise = FastNoise::PerlinFractal.new
  $noise.seed = $seed
  $noise.fractal_octaves = 2
  #$noise.fractal_lacunarity = 3.0
  #$noise.fractal_gain = 0.01
  #$noise.frequency = 0.5
  $noise_x_multiplier = 1
end

def run
  set_globals

  stripe_rects = (0..$width).step($stripe_width*2).map do |x|
    [[x,0], [x+$stripe_width,0], [x+$stripe_width, $height], [x, $height], [x, 0]]
  end
  stripe_rects_t = stripe_rects.map { _1.map { |p| [p[0]+$stripe_offset, p[1]] } }

  clip_polys = build_polys
  polys_even = clip_polys.each_with_index.select { _2.even?  }.map(&:first)
  polys_odd = clip_polys.each_with_index.select { _2.odd?  }.map(&:first)

  clipped = [[polys_even, stripe_rects], [polys_odd, stripe_rects_t]].map do |polys, stripes|
    polys.map do |poly|
      stripes.map do |s|
        c = Clipper::Clipper.new
        c.add_subject_polygon(s)
        c.add_clip_polygon(poly)
        c.intersection :non_zero, :non_zero
      end.reject(&:empty?).flatten(1)
    end.flatten(1)
  end

  f = build_filename
  puts f
  save(f, clipped)
  #save("a.svg", clipped)
end

def build_polys
  y0 = rand(40)
  curves = [ [[0, 0], [$width, 0]] ]
  while y0 < $height
    curves << (0..$width).map do |x|
      [x, mapn($noise.noise(x*$noise_x_multiplier, $yoff), -1, 1, y0, y0+200)]
    end
    y0 += rand(200)
    $yoff += rand(1000)
  end
  curves << [[0, $height], [$width, $height]]

  curves.zip(curves[1..-1])[0...-1].map { |a, b| a + b.reverse + [a.first] }
end

def build_filename
  stamp = Time.now.strftime("%Y-%m-%d_%H%M%S_%6N")
  File.join(
    File.expand_path($out_path),
    "offsetstripes_#{stamp}_s#{$seed}.svg"
  )
end

class Offset < Mustache
  self.template_path = __dir__
end

def save(filename, clipped)
  path_layers = clipped.map do |polys_layer|
    polys_layer.map do |poly|
        s = poly.map { |p| "#{p[0]} #{p[1]}" }.join(" ")
        %(<path d="M#{s} Z" fill="black" stroke="black"/>)
    end.join("\n")
  end

  t = Offset.new
  t[:groups] = path_layers.map do |paths|
    <<~END
      <g inkscape:label="stripes" inkscape:groupmode="layer" stroke="none" transform="translate(40,57)">
          #{paths}
        </g>
    END
  end.join("\n")
  t[:width] = $width
  t[:height] = $height

  open(filename, 'w') { |f| f << t.render }
end

def lerp(start, stop, step)
  (stop * step) + (start * (1.0 - step))
end

def inv_lerp(start, stop, value)
  (value - start) / (stop - start)
end

def mapn(value, from_start, from_end, to_start, to_end)
  lerp(to_start, to_end, inv_lerp(from_start, from_end, value))
end

OptionParser.new do |opts|
  opts.banner = "Usage: offset.rb [-o PATH] [-n NUM_RUNS]"

  opts.on("-h", "--help", "Halp") do
    puts opts
    exit
  end

  opts.on("-o", "--output-folder [PATH]", "Path to output folder [default: #{$out_path}]") do |p|
    $out_path = p
  end
  opts.on("-n", "--num-runs [N]", "Number of images to generate [default: #{$num_runs}]") do |n|
    $num_runs = n.to_i
  end
  opts.on("-s", "--seed [N]", "RNG seed") { |n| $seed = n.to_i }
end.parse!

puts "seed: #{$seed}\n"
$num_runs.times { run }
