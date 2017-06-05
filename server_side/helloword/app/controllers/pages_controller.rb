class PagesController < ApplicationController 
  # For details on the DSL available within this file, see http://guides.rubyonrails.org/routing.html

  skip_before_action  :verify_authenticity_token

 def home
	@greetings = "This is something new"
    puts "Yep, home! Page"
  end

  def create
    # sleep(30)

    # a = request.body.read
    # binding.pry

  	# p "hit here"
    puts params
    a = request.body.read
    b = a.split("\r\n\r\n")[1]
    File.open("ash", 'wb') {|f| f.write(b)}


  	return "Success"
  end

end
