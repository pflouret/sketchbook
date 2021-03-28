require 'json'
require 'ostruct'
require 'pry'
require 'pry-byebug'
require 'set'

require 'rgl/adjacency'
require 'rgl/connected_components'
require 'rgl/dot'
require 'rgl/traversal'

ZOOM = 19
NAIVE_MERGE = false
OUT_FILENAME = 'out.svg'
OUT_REF_FILENAME = 'out-ref.svg'

ALLOWED_HIGHWAY_TYPES = [
  'living_street',
  'motorway',
  'primary',
  'residential',
  'secondary',
  'service', # (filter n of nodes)
  'tertiary',
  'trunk',
  'footway',
  'pedestrian',
  #'construction',
  #'corridor',
  #'cycleway',
  #'motorway_link', # maybe
  #'path',
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

def merge(ways_by_id)
  # Merge all the different segments of the same way
  ways_by_id.map do |id, way_group|
    # (first,last) nodes -> way object
    edge_index = way_group.reduce({}) do |acc, s|
      if s.nodes.first == s.nodes.last
        # Just get rid of closed trails, too lazy to deal with properly.
        s.nodes.pop
      end
      acc.merge!([s.nodes.first, s.nodes.last].to_set => s)
    end

    g = RGL::AdjacencyGraph.new
    g.add_edges(*edge_index.keys.map(&:to_a))

    seen = Set.new
    node_paths = []
    g.each_connected_component do |vertices|
      # Traverse and collect a trail
      forks = [vertices.find { |v| g.out_degree(v) == 1 } || vertices.first]
      until forks.empty?
        v = forks.shift
        vertex_path = [v]
        until v.nil?
          seen << v
          adj = g.adjacent_vertices(v).reject { |w| seen.include?(w) }
          cycle_vertex = g.adjacent_vertices(v).find { |w| vertex_path[0...-2].include?(w) }
          unless cycle_vertex.nil?
            # Add the segment that completes the cycle.
            node_paths << build_node_path([v, cycle_vertex], edge_index)
          end
          forks << v if adj.size > 1
          v = adj.shift
          vertex_path << v unless v.nil?
        end

        node_paths << build_node_path(vertex_path, edge_index)
      end
    end
    OpenStruct.new(id: id, node_paths: node_paths.reject(&:nil?))
  end
end

def build_node_path(vertex_path, edge_index)
  # Group as (first,last) nodes
  segments = vertex_path
    .zip(vertex_path[1..-1])[0...-1]
    .map { |a| edge_index[a.to_set] }
    .reject(&:nil?)

  return if segments.empty?

  # Merge all segments into the first (they might've been reversed)
  first_segment = segments.first.nodes.first == vertex_path.first ?
    segments.first.nodes:
    segments.first.nodes.reverse
  segments[1..-1].reduce(first_segment) do |a, s|
    a + (a.last == s.nodes.first ? s.nodes : s.nodes.reverse)
  end
end

def save(filename, merged_ways)
  open(filename, 'w') do |f|
    f << <<~END
      <?xml version="1.0" encoding="utf-8"?>
      <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
      <style>path { vector-effect: non-scaling-stroke; } </style>
      <g fill="none" stroke-width="0.6" stroke="#1a1a1a">
    END

    points = merged_ways.map(&:node_paths).flatten(2)
    xx, yy = points.map { |p| p[0] }, points.map { |p| p[1] }
    min_x, min_y = xx.min, yy.min

    merged_ways.each do |mw|
      mw.node_paths.each do |path|
        s = path.map { |p| "#{p[0]-min_x} #{p[1]-min_y}" }.join(' ')
        f << %(<path id="#{mw.id}" d="M#{s}"/>\n)
      end
    end
    f << "</g></svg>"
  end
end

def save_flat(filename, node_paths)
  open(filename, 'w') do |f|
    f << <<~END
      <?xml version="1.0" encoding="utf-8"?>
      <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
      <style>path { vector-effect: non-scaling-stroke; } </style>
      <g fill="none" stroke-width="0.6" stroke="#1a1a1a">
    END

    points = node_paths.flatten(1)
    xx, yy = points.map { |p| p[0] }, points.map { |p| p[1] }
    min_x, min_y = xx.min, yy.min

    node_paths.each do |path|
      s = path.map { |p| "#{p[0]-min_x} #{p[1]-min_y}" }.join(' ')
      f << %(<path d="M#{s}"/>\n)
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

def naive_merge(merged_ways)
  node_paths = merged_ways.map(&:node_paths).flatten(1)
  while true do
    matches = node_paths.reduce({}) do |m, np|
      start_p = np.first
      end_p = np.last

      other = node_paths.find do |other|
        other != np && (
          other.first == start_p ||
          other.first == end_p ||
          other.last == start_p ||
          other.last == end_p
        )
      end

      other.nil? ? m : m.merge!(np => other)
    end

    puts matches.size
    break if matches.empty?

    seen = Set.new
    matches.each do |np1, np2|
      next if seen.intersect?([np1, np2].to_set)
      seen << np1
      seen << np2

      start_p1, end_p1 = np1.first, np1.last
      start_p2, end_p2 = np2.first, np2.last

      if end_p1 == start_p2 || end_p2 == start_p1
        np1, np2 = np2, np1 if end_p2 == start_p1
        np1.push(*np2[1..-1])
      elsif end_p1 == end_p2
        np1.push(*np2[0..-2].reverse)
      elsif start_p1 == start_p2
        # reverse "vector" direction of np1 and add np2 to the end
        np1.reverse!
        np1.push(*np2[1..-1])
      end
      node_paths.delete(np2)
    end
  end

  node_paths
end




data = open(ARGV[0]) { |f| JSON.parse(f.read, symbolize_names: true) }[:elements]
  .map(&OpenStruct.method(:new))

ways = data
  .select { |d| d.type == 'way' && d.tags[:name] && ALLOWED_HIGHWAY_TYPES.include?(d.tags[:highway]) }
  .reject { |d| d.tags[:highway] == 'service' && d.nodes.size < 10 }
  .reject { |d| d.tags[:highway] == 'pedestrian' && d.nodes.size < 10 }
  .reject { |d| d.tags[:highway] == 'footway' && d.nodes.size < 10 }

nodes = data
  .select { |d| d.type == 'node' }
  .reduce({}) { |m, d| m.merge!(d.id => d) }

ways_by_id = ways
  .map { |w| OpenStruct.new(id: w.tags[:name], nodes: w.nodes) }
  .group_by(&:id)

merged_ways = merge(ways_by_id)

# Convert path from node_id to mercator-projected [x,y]
if NAIVE_MERGE
  node_paths = naive_merge(merged_ways).map do |path|
    path.map { |n| project(nodes[n].lat, nodes[n].lon, ZOOM) }
  end
  save_flat(OUT_FILENAME, node_paths)
else
  ref_ways = ways.map do |mw|
    mw.nodes.map { |n| project(nodes[n].lat, nodes[n].lon, ZOOM) }
  end
  save_flat(OUT_REF_FILENAME, ref_ways)

  merged_ways.each do |mw|
    mw.node_paths = mw.node_paths.map do |path|
      path.map { |n| project(nodes[n].lat, nodes[n].lon, ZOOM) }
    end
  end
  save(OUT_FILENAME, merged_ways)
end
