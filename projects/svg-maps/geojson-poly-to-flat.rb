require 'json'

gj = JSON.parse(ARGF.read)
puts gj["coordinates"].first.map { |e| [e[1], e[0]] }.flatten.join(' ').inspect
