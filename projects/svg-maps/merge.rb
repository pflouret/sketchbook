require 'bundler/inline'

gemfile do
  gem 'savage', git: 'https://github.com/awebneck/savage.git'
  gem 'pry-byebug', require: 'pry'
end

module Savage
  class Path
    def save(filename)
      open(filename, 'w') do |f|
        f << <<~END
             <?xml version="1.0" encoding="utf-8"?>
             <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
             <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 2700 1168">
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

path.subpaths = path.subpaths.select { |sp| sp.directions.size >= 2 }

while true do
  matches = path.subpaths.reduce({}) do |m, sp|
    start_p = sp.directions.first.target
    end_p = sp.directions.last.target

    other = path.subpaths.find do |other|
      other != sp && (
        other.directions.first.target == start_p ||
        other.directions.first.target == end_p ||
        other.directions.last.target == start_p ||
        other.directions.last.target == end_p
      )
    end

    other.nil? ? m : m.merge!(sp => other)
  end

  puts matches.size
  break if matches.empty?

  seen = Set.new
  matches.each do |sp1, sp2|
    next if seen.intersect?([sp1, sp2].to_set)
    seen << sp1
    seen << sp2

    start_p1, end_p1 = sp1.directions.first.target, sp1.directions.last.target
    start_p2, end_p2 = sp2.directions.first.target, sp2.directions.last.target

    if end_p1 == start_p2 || end_p2 == start_p1
      sp1, sp2 = sp2, sp1 if end_p2 == start_p1
      sp1.directions += sp2.directions[1..-1]
    elsif end_p1 == end_p2
      sp1.directions += sp2.directions[0..-2].reverse
      p = sp1.directions[-1].target
      sp1.directions[-1] = Savage::Directions::LineTo.new(p.x, p.y)
    elsif start_p1 == start_p2
      # reverse "vector" direction of sp1 and add sp2 to the end
      sp1.directions = sp1.directions.reverse
      first, last = sp1.directions.first.target, sp1.directions.last.target
      sp1.directions[0] = Savage::Directions::MoveTo.new(first.x, first.y)
      sp1.directions[-1] = Savage::Directions::LineTo.new(last.x, last.y)
      sp1.directions += sp2.directions[1..-1]
    end
    path.subpaths.delete(sp2)
  end
end

path.save('out.svg')

#binding.pry
exit (0)
puts

#by_start = path.subpaths.group_by { |p| p.directions.first.target }
#by_end = path.subpaths.group_by { |p| p.directions.last.target }
#
#matches2 = path.subpaths.map do |sp|
#  start_p = sp.directions.first.target
#  end_p = sp.directions.last.target
#  other = by_start[start_p] || by_start[end_p] || by_end[
#end

def crap
  while true do
    subpaths = path.subpaths.select { |p| p.directions.size >= 2 }

    by_start = subpaths.group_by { |p| p.directions.first.target }
    by_end = subpaths.group_by { |p| p.directions.last.target }

    end_matches = by_end.select { |k, v| by_start.include?(k) }
    end_matches.each do |p, end_subpaths|
      start_subpath = by_start[p].first
      end_subpaths.first.directions += start_subpath.directions[1..-1]
      path.subpaths.delete(start_subpath)
    end

    start_matches = by_start.select { |k, v| by_end.include?(k) }
    start_matches.each do |p, start_subpaths|
      end_subpath = by_end[p].first
      sp = start_subpaths.first
      start_p = sp.directions.shift.target
      sp.directions.unshift(Savage::Directions::LineTo.new(start_p.x, start_p.y))
      sp.directions = end_subpath.directions + sp.directions
      path.subpaths.delete(sp)
    end

    break if end_matches.empty? && start_matches.empty?
  end
end

