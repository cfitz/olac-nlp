#/usr/bin/env ruby

include Java 
import java.util.ArrayList

require 'open-uri'
require 'tire'

require 'olac.jar'
require 'marc'
require 'stringio'
require 'diffy'
require 'csv'


class MarcProcessor

  OpenSSL::SSL::VERIFY_PEER = OpenSSL::SSL::VERIFY_NONE
  
  
  attr_accessor :nlp_processor
  attr_accessor :marc_output
  attr_accessor :marc
  attr_accessor :role_codes
  DISREGARD_TERMS = [ "special", "bonus"]
  SEARCH_TERMS = ["air", "aired", "broadcast", "copyright", "filmed", "motion", "originally", "produced", "production",
                  "recorded", "live", "telecast", "film", "released", "televised", "made", "performed", "premiered", 
                  "presented", "produced", "production", "recorded", "released", "videotaped", "videotape", "taped",
                  "interviews"]
  
  def initialize(marc_file = 'test.mrc')
   @marc = MARC::Reader.new(marc_file)
   @role_codes = getRoleCodes
   people = ArrayList.new
   
   @marc.each do |marc|
    if marc["700"]
      people << marc["700"]["a"] if marc["700"]["a"]
    end
   end
  
   @marc = MARC::Reader.new(marc_file) # because it's a read once object, we have to remake it.
   @nlp_process =  org.olac.nlp.Processor.new(people)
  end
  
  def nullify
    @nlp_process.nullify
    @nlp = nil
  end
  
  def warmup
    if @nlp_processor.nil?
      @nlp_process =  org.olac.nlp.Processor.new 
    end
  end
  
  
  def annotate(text)
   # warmup
    nlp_results = @nlp_process.process(text)
    results = { "dates" => [], "people" => {}, "found_terms" => []}
    
    nlp_results.dates.each do |d| 
      if d.include?("/") # some dates are returned with a slash i.e. 1995/1996
        d.split("/").each { |dd| results["dates"] << dd }
      else # no slash, just put it in the retuls
        results["dates"] << d
      end
    end
        
    nlp_results.people.each_pair { |name, roles| results["people"][name] = roles.to_a }
    

    DISREGARD_TERMS.each { |term| results["dates"] = [] if ( text.split(" ").first == term ) }
    SEARCH_TERMS.each { |term| results["found_terms"] << term if text.downcase.include?(term) }
    if  results["found_terms"].length < 1
      results["dates"] = []
    end
    
    results["found_terms"].uniq!
    results["dates"].uniq!    
    results
   
  
  end
  
  
  def process( )   
    file_index = 0
    @marc.each do |record|
     record_string = record.to_s
     File.open("records/#{file_index.to_s}_old.txt", "w") { |f| f << record_string }

     append_to_marc(record, processField008(record["008"])) 
     
     field_index  = 1
     record.each_by_tag("033") { |tag| append_to_marc(record, processField033(tag, field_index) ); field_index+=1 }
     
     field_index = 1
     record.each_by_tag("046") { |tag| append_to_marc(record, processField046(tag, field_index)); field_index+=1 }
     
     field_index = 1
     record.each_by_tag("245") { |tag| append_to_marc(record, processField245(tag, field_index)); field_index+=1}
     
     
     field_index = 1
     record.each_by_tag("500") { |tag| append_to_marc( record, processField500s(tag, field_index)); field_index+=1 }
     
     field_index = 1
     record.each_by_tag("518") { |tag| append_to_marc(record, processField500s(tag, field_index)); field_index+=1 }
     
     
     
     diff = Diffy::Diff.new(record_string, record.to_s ).to_s(:html_simple)
     File.open("records/#{file_index.to_s}_new.txt", "w") { |f| f << record.to_s }
     File.open("records/#{file_index.to_s}_diff.html", "w") { |f| f << diff }

     file_index+=1

    end  
  end
    
   
  
  def append_to_marc(record, field= nil)
    unless field.nil?
      if field.is_a?(Array)
        field.each { |f| record.append(f)}
      else
        record.append(field)
      end
    end
    
    return record
  end
  
  
  # this processes the 008 field. We change the values from the 6..15, which can be a range. 
  def processField008(field)
    return nil if field.nil?
    field = field.value[6..15]
    if field[0] == "p" or field[0] == "r"
              
      first_date = field[1..4]
      second_date = field[5..9]
      start_date = first_date.gsub("u", "9").to_i
      end_date = second_date.gsub("u", "9").to_i
      
      if ( start_date > end_date && !second_date.include?("u") )
        # if the secondDate is lower and there was not a "u" in the date field.
        return MARC::DataField.new('980', '',  ' ', ['a', end_date], ['b', "008"], ['c', field[0] ])
      elsif  ( start_date > end_date && second_date.include?("u") ) 
        # if the 2nd date is lower and there was a 'u', this means we need to add a date range.
        start_date = end_date - 9
        return MARC::DataField.new('980', '',  ' ', ['a', "#{start_date}-#{end_date}"], ['b', "008"], ['c', field[0] ])
      elsif ( first_date.include?("u") )
        # first date is larger, but there's a 'u' so, it needs a range. 
        end_date = start_date - 9
        return MARC::DataField.new('980', '',  ' ', ['a', "#{start_date}-#{end_date}"], ['b', "008"], ['c', field[0] ])
      else
        # at this point, we assume first date was larger and no 'u' so we just add the first date
        return MARC::DataField.new('980', '',  ' ', ['a', start_date], ['b', "008"], ['c', field[0] ])
      end

    end
  end #processField008
  
  
  
  # this processes the 033, which can have a varety of date formats.
  def processField033(field, index)
    field.find_all do |subfield|
    
      if subfield.code == "a"
        data = subfield.value.gsub(/[^0-9]/, "")
       
        if data.length == 4 # a simple 4-digit year
          date = data
        elsif data.length == 5 # this is a year and month, 19781
          date = "#{data[0..3]}-0#{data[4]}"
        elsif data.length == 6 # a year with a 2 digit date
          date = "#{data[0..3]}-#{data[4..5]}" # 4 digit year, two digit month, one digit day
        elsif data.length == 7
    		  date = "#{data[0..3]}-#{data[4..5]}-0#{data[6]}"
    		elsif data.length == 8 # 4 year, 2 month, 2 day
    		  date = "#{data[0..3]}-#{data[4..5]}-#{data[6..7]}"
    	  end
	  
    	  if date
    	    return MARC::DataField.new('980', '',  ' ', ['a', date], ['b', "033"], ['c', "1" ], [ "d", "a"], [ "e", index])
        else 
          nil
        end
      end
    
    end
  end
   
  # this processes the 046, whihc is a plain old date we can stick into 980
  def processField046(field, index)
    field.find_all do |subfield|
      if subfield.code == "k"
        return MARC::DataField.new("980", "", "", ['a', subfield.value ], ["b", "046"], ["c", "1"], ["d", "k"], ["e", index])
      end
    end
  end
  
  
  def processField245(field, index)
    results = []
    field.find_all do |subfield|
      if subfield.code == "c"
       text = cleanField(subfield.value) 
       terms = text.split(";")
       terms.each_with_index { |t, i| results << MARC::DataField.new('901', '',  ' ', ['a', t], ['b', "245c"], ['c', 1 ], ["d", i]) }
       terms.each_with_index { |t, i| results << checkRole(t, i)  }
      end
    end
    results.flatten!
    results.compact
  end 
  
  
  
  
  
  # processes the 500 field
  def processField500s(field, index)
    dataFields = []
    field.find_all do |subfield| 
      if subfield.code == "a"
        text = cleanField(subfield.value)
        results = annotate(text)
        unless results.nil? 
          
          if results["found_terms"].length > 0 # for each found term present, we add a subfield for each date...
            results["dates"].each_with_index do |date, date_index| # iterate through the dates and make them into 981
              df = MARC::DataField.new("981", "", "", ["a", date ], [ "b", field.tag], ["c", index], ["e", "a"], ["f", date_index] ) 
              if field.tag == "500" # we add all the terms a d subfiled to the 500 field, but not the others. 
                results["found_terms"].each { |ft| df.subfields << MARC::Subfield.new("d", ft )  }
              end
              dataFields << df
            end
          end
        end
      end
    end
    dataFields
  end
  
  
  # this check is the name has a role, returns nil if the name doesn't have a role.
  def checkRole(text, index)
    results = []
    nlp = annotate(text)
    people = nlp["people"]
    people.each_pair do |label, roles |    
      roles.each { |r|  results << MARC::DataField.new('902', '',  ' ', ['a', r], ['b', roleCode(r)], ['c', label ], ["e", '245c'])    }
    end
    puts text
    puts results
    return results.compact 
  end
  
  # the cleans up with text 
  def cleanField(text = "")
    text.gsub!("Twentieth Century", "") # date parser hates this...
    text.gsub!("-"," - ")
    
    text.scan(/(\b\d{4}s)/).each {|s| text.gsub!(s.first, convertDecades(s.first) )} #we need to convert decades with s'es (1930s) into ranges. 
    text.scan(/(c\d{4})/).each { |s| text.gsub!(s.first, s.first.delete("c") )} #we need to remove c in copyright dates c1990 
    text.scan(/(\d{4},)/).each {|s| text.gsub!(s.first, s.first.delete(",") )} #we need to remove commas after dates, like 1990,
    text.scan(/(\d\/)\d{4}/).each { |p| text.gsub!( p.first, "0#{p.first.gsub("/", "-")}" )   }
    
    
    text
    
  end
  
  # this converts "1930s" into "1930/1939"
  def convertDecades(decade="") 
	  decade.gsub!("s","")
	  decadeInt = decade.to_i + 9
	  "#{decade}-#{decadeInt.to_s}"
	end
  
  
  # this get the role codes from the gspreadsheet and returns a hash.
  def getRoleCodes
    results = {}
    roles = CSV.parse(open("https://docs.google.com/spreadsheet/pub?key=0AjRF7huQ7tqKdDFwaElEaklEOXNVS0R6eEFEcE84NUE&single=true&gid=0&output=csv"), :headers => true)
    roles.each { |r| results[r["Function"]] = r["code"] }
    results
  end
  
  def roleCode(code)
    role_codes[code] ? role_codes[code] : code
  end
  
  
end



if __FILE__ == $0
  mp = MarcProcessor.new("test.mrc")
  mp.process
end



