require 'bundler/inline'

gemfile do
  source 'https://rubygems.org'
  gem 'savage', git: 'https://github.com/awebneck/savage.git'
  gem 'pry', '~> 0.13.1'
  gem 'rgl', '~> 0.5.7'
end

require 'rgl/adjacency'
require 'rgl/connected_components'
require 'rgl/traversal'

module Savage
  class Path
    def save(filename, viewbox="0 0 2700 1168")
      open(filename, 'w') do |f|
        f << <<~END
             <?xml version="1.0" encoding="utf-8"?>
             <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
             <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="#{viewbox}">
             <style>path { vector-effect: non-scaling-stroke; } </style>
             <g fill="none" stroke-width="0.6" stroke="#1a1a1a">
        END
        @subpaths.reject { |sp| sp.directions.empty? }.each do |sp|
          f << %(<path d="#{sp.to_command}"/>\n)
        end
        f << "</g></svg>"
      end
    end
  end
end


path = open(ARGV[0]) { |f| Savage::Parser.parse(f.read) }
subpaths = path.subpaths.reject { |sp| sp.directions.empty? }

segments = subpaths.map do |sp|
  sp.directions
    .zip(sp.directions[1..-1])[0...-1]
    .map { |a| a.map(&:target) }
#    .map do |s|
#      segment = Savage::SubPath.new(s[0].x, s[0].y)
#      segment.line_to(s[1].x, s[1].y)
#      segment
#    end
end.flatten(1)

g = RGL::AdjacencyGraph.new
g.add_edges(segments)


#by_start = segments.group_by(&:first)
#by_end = segments.group_by(&:last)

#segments.sort! { |s1, s2| s1[0].x == s2[0].x ? s1[0].y <=> s2[0].y : s1[0].x <=> s2[0].x }

#path.subpaths = segments
#path.save('out.svg', '0 0 15 15')

puts
binding.pry
exit(0)

def make_test_svg
  path = Savage::Path.new do |p|
    p.move_to 0, 1
    p.line_to 3, 0
    p.line_to 3, 2
    p.line_to 4, 2
    p.line_to 5, 3
    p.line_to 6, 3
  end

  p = path.move_to 6, 3
  p.line_to 6, 1
  p.line_to 8, 1
  p.line_to 8, 0
  p.line_to 9, 0

  p = path.move_to 9, 6
  p.line_to 9, 5
  p.line_to 10, 5
  p.line_to 10, 4
  p.line_to 9, 4
  p.line_to 9, 0

  path.save('test.svg', "0 0 20 20")
end

