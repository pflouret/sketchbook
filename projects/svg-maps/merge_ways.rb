require 'json'
require 'ostruct'
#require 'pry'
#require 'pry-byebug'
require 'set'

require 'rgl/adjacency'
require 'rgl/connected_components'
require 'rgl/dot'
require 'rgl/traversal'

ZOOM = 19

ALLOWED_HIGHWAY_TYPES = [
  'living_street',
  'motorway',
  'primary',
  'residential',
  'secondary',
  'service', # (filter n of nodes)
  'tertiary',
  'trunk',
  #'construction',
  #'corridor',
  #'cycleway',
  #'footway',
  #'motorway_link', # maybe
  #'path',
  #'pedestrian',
  #'platform',
  #'primary_link',
  #'proposed',
  #'raceway',
  #'secondary_link',
  #'services',
  #'steps',
  #'tertiary_link',
  #'track', # tracktype=grade3 ?
  #'trunk_link',
  #'unclassified'
].to_set.freeze

def save(filename, paths)
  open(filename, 'w') do |f|
    f << <<~END
      <?xml version="1.0" encoding="utf-8"?>
      <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="#{viewbox_str(paths)}">
      <style>path { vector-effect: non-scaling-stroke; } </style>
      <g fill="none" stroke-width="0.6" stroke="#1a1a1a">
    END
    paths.each do |name, pp|
      pp.each do |path|
        s = path.map { |p| "#{p[0]} #{p[1]}" }.join(' ')
        f << %(<path id="#{name}" d="M#{s}"/>\n)
      end
    end
    f << "</g></svg>"
  end
end

def viewbox_str(paths)
  points = paths.values.flatten(2)
  xx, yy = points.map { |p| p[0] }, points.map { |p| p[1] }
  min, max = [xx.min, yy.min], [xx.max, yy.max]
  [min[0], min[1], max[0]-min[0], max[1]-min[1]].join(' ')
end

def project(lat_deg, lng_deg, zoom)
  lat_rad = lat_deg/180 * Math::PI
  n = 2.0 ** zoom
  x = ((lng_deg + 180.0) / 360.0 * n)
  y = ((1.0 - Math::log(Math::tan(lat_rad) + (1 / Math::cos(lat_rad))) / Math::PI) / 2.0 * n)
  [x, y]
end

data = open(ARGV[0]) { |f| JSON.parse(f.read, symbolize_names: true) }[:elements]
  .map(&OpenStruct.method(:new))

ways = data
  .select { |d| d.type == 'way' && d.tags[:name] && ALLOWED_HIGHWAY_TYPES.include?(d.tags[:highway]) }
  .reject { |d| d.tags[:highway] == 'service' && d.nodes.size < 10 }

nodes = data
  .select { |d| d.type == 'node' }
  .reduce({}) { |m, d| m.merge!(d.id => d) }

wg = ways.group_by { |d| d.tags[:name] }

node_paths = wg.map do |name, way_segments|
  # (first,last) nodes -> way object
  edge_index = way_segments.reduce({}) do |acc, s|
    if s.nodes.first == s.nodes.last
      s.nodes.pop
    end
    acc.merge!([s.nodes.first, s.nodes.last].to_set => s)
  end

  g = RGL::AdjacencyGraph.new
  g.add_edges(*edge_index.keys.map(&:to_a))

  paths = []
  g.each_connected_component do |vertices|
    # Traverse and collect a trail
    start = vertices.find { |v| g.out_degree(v) == 1 } || vertices.first
    node_path = g.dfs_iterator(start).to_a

    # Group as (first,last) nodes
    segments = node_path
      .zip(node_path[1..-1])[0...-1]
      .map { |a| edge_index[a.to_set] }
      .reject(&:nil?)

    # Merge all segments into the first (they might've been reversed)
    paths << segments[1..-1].reduce(segments.first.nodes) do |a, s|
      a + (a.last == s.nodes.first ? s.nodes : s.nodes.reverse)
    end
  end
  [name, paths]
end.to_h

svg_paths = node_paths.map do |name, paths|
  [
    name,
    paths.map do |path|
      path.map { |n| project(nodes[n].lat, nodes[n].lon, ZOOM) }
    end
  ]
end.to_h

save('out.svg', svg_paths)
