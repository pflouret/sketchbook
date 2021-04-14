require 'color'
require 'json'
require 'mustache'
require 'ostruct'
require 'pry'
require 'pry-byebug'
require 'set'

ZOOM = 19

def build_draw_objs(data_path)
  data = open(data_path) { |f| JSON.parse(f.read, symbolize_names: true) }[:elements]
    .map(&OpenStruct.method(:new))

  $nodes = data
    .select { |d| d.type == 'node' }
    .reduce({}) { |m, d| m.merge!(d.id => d) }

  $ways = data
    .select { |d| d.type == 'way' }
    .reduce({}) { |m, d| m.merge!(d.id => d) }

  rels = data.select { |d| d.type == 'relation' }
  rels_by_ref = rels.group_by { |r| r.tags[:ref] }
  rels_by_ends = (rels_by_ref.delete(nil) || [])
    .group_by { |r| [r.tags[:from], r.tags[:to]].to_set }

  basename = File.basename(data_path, '.json')
  rels_by_ref.map { build_rel_draw_objs(_2.first, basename) }.flatten(1) +
    rels_by_ends.map { build_rel_draw_objs(_2.first, basename) }.flatten(1)
end

def build_rel_draw_objs(rel, basename)
  lines = rel.members
    .select { |e| e[:type] == 'way' && e[:role].empty? }
    .map { |e| $ways[e[:ref]] }
    .map { |w| w.nodes.map { |n| $nodes[n] } }
    .reduce([]) do |a, nodes|
      if a.last&.last == nodes.first
        a[-1] += nodes[1..-1]
      else
        a << nodes
      end
      a
    end

  OpenStruct.new(
    rel: rel,
    basename: basename,
    lines: lines
  )
end

class Subway < Mustache
  self.template_path = __dir__
end

def save(filename, all_draw_objs)
  x_offset = 0
  groups = all_draw_objs.map do |draw_objs|
    draw_objs.each do |o|
      o.lines_p = o.lines.map do |path|
        path.map { |n| project(n.lat, n.lon, ZOOM) }
      end
    end

    points = draw_objs.map(&:lines_p).flatten(2)
    xx, yy = points.map { |p| p[0] }, points.map { |p| p[1] }
    min_x, min_y, max_x = xx.min, yy.min, xx.max

    draw_objs.sort_by! { -_1.label.to_i }
    paths = draw_objs.map do |o|
      o.lines_p.map do |path|
        s = path.map { |p| "#{p[0]-min_x+x_offset} #{p[1]-min_y}" }.join(' ')
        %(<path d="M#{s}"/>)
      end.join("\n    ")
    end.join("\n")
    x_offset += (max_x-min_x) + 30
    <<~END
        <g inkscape:label="#{draw_objs.first.basename}" inkscape:groupmode="layer" stroke="black" stroke-width="1" fill="none">
            #{paths}
          </g>
    END
  end

  t = Subway.new
  t[:groups] = groups.join("\n")
  open(filename, 'w') { |f| f << t.render }
end

def project(lat_deg, lng_deg, zoom)
  lat_rad = lat_deg/180 * Math::PI
  n = 2.0 ** zoom
  x = ((lng_deg + 180.0) / 360.0 * n)
  y = ((1.0 - Math::log(Math::tan(lat_rad) + (1 / Math::cos(lat_rad))) / Math::PI) / 2.0 * n)
  [x, y]
end

all_draw_objs = ARGV.map(&method(:build_draw_objs))
save("subway-all.svg", all_draw_objs)
