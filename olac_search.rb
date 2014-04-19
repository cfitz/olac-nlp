require 'csv'
require 'tire'
require 'open-uri'
class OlacSearch
  
  # this is a stupid hacky fix.
  OpenSSL::SSL::VERIFY_PEER = OpenSSL::SSL::VERIFY_NONE
   
  attr_accessor :search_functions
  attr_accessor :index
  
  
  def initialize(google_doc_url = "https://docs.google.com/spreadsheet/pub?key=0AjRF7huQ7tqKdDFwaElEaklEOXNVS0R6eEFEcE84NUE&single=true&gid=1&output=csv")
    @search_functions = parse_doc(google_doc_url)
    @index = Tire::Index.new('index')
    prepare_index
  end
  
  # this indexes a block of text into the index
  def index(text = "")
    prepare_index
    @index.store :text => text,  :analyzer => 'snowball'
    @index.refresh    
  end 
  
  
  # now this runs all the names against the algorithm to find if we have a match
  def search(text = "")
    functions = []
    @search_functions.each_pair  do |function, values|
      values.each do |val|
          term = val.gsub("x1", text)
          term = "\"#{term}\"~3"
          results = Tire.search 'index' do
            query do 
                string term
            end
          end
          functions << function if (results.results.total > 0 )
      end
      
    end
    functions.uniq
  end
  
  
  
  
  private
  
  
  
  # this gets the csv from the google doc that has teh functions listed and returns a hash with the results.
  def parse_doc(url)
    search_functions = {}
    csv = CSV.parse(open(url).read, :headers => true)
    csv.each do |row|
      function = row["function"]
      search_functions[function] ||= []
      search_functions[function] << row["name_first"] unless row["name_first"].nil?
      search_functions[function] << row["name_last"] unless row["name_last"].nil?
      search_functions[function] << row["name_middle"] unless row["name_middle"].nil?
    end
    search_functions
  end
  
  #this just gets the index all read for us
  def prepare_index
    @index.delete
    @index.create
  end
  
  
  
end



if __FILE__ == $0
  searcher = OlacSearch.new
  searcher.index("Director, Charlotte Zwerin ; producers, Charlotte Zwerin, Bruce Ricker ; executive producer, Clint Eastwood")
  
  results = searcher.search("Charlotte Zwerin") #"Bill Smith, director of animation"~3
  puts "Here are your results for  Charlotte Zwerin : #{results}"
  
  results = searcher.search("Bruce Ricker") #"Bill Smith, director of animation"~3
  puts "Here are your results for  Bruce Ricker : #{results}"
  
  results = searcher.search("Clint Eastwood") #"Bill Smith, director of animation"~3
   puts "Here are your results for Clint Eastwood : #{results}"
  
end



