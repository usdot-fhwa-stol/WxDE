/*
 * @author rothr
 * @desc Scripts that helps navigating the
 * website using the keyboard.
 */
( function($) {
	//variable that will contain the html codes to be appended to the 
	//accessibility navigation menu
	var html = '';
	//cache body which tells us under which menu item/page we are
	//we get the class for unique accessibility instructions
	var body = $('body').attr('class'); // e.g. "home-page"
	//condition that checks for page specific navigation items to be
	//appended to the accessibility navigation menu
	switch( body ) {
		case 'home-page':
			html += '<a href="" class="skip-link pre-menu" data-skip="latestNews">Skip to Latest News</a>' +
			   		'<a href="" class="skip-link pre-menu" data-skip="mainContent">Skip to Main Content</a>';
			break;
		//------------	
		case 'summary-page':
			html +=	'<a href="" class="skip-link pre-menu" data-skip="statesList">Skip to State List</a>' +
					'<a href="" class="skip-link pre-menu" data-skip="map-canvas">Skip to Summary Map</a>';
			break;
		//------------	
		case 'faq-page':
			html += '<a href="" class="skip-link pre-menu" data-skip="cat-1">Skip to FAQ</a>';
			break;
		//------------	
		case 'wde-map-page':
			html += '<a href="" class="skip-link pre-menu" data-skip="map_canvas">Skip to Map</a>';
			break;
		//------------	
		case 'show-query-results':
			html += '<a href="" class="skip-link pre-menu" data-skip="fullscreen-button">Skip to Page Body</a>';
			break;
		//------------
	}// switch
	
	//Add the skip to footer, every page has a footer.
	html += '<a href="#" class="skip-link pre-menu" data-skip="footer-link">Skip to Local Footer</a>';
	//Finally append the navigation items to the accessibility navigation menu
	$('#accessibility-menu').append( html );
	
	//Add the function of focusing to an element defined in the
	//data-skip property of the classified link
	$('.skip-link, .skip-to-section').on('click', function(evt) {
		evt.preventDefault();
		evt.stopPropagation();
		
		var skip = $(this).data('skip');
		
		$('#' + skip).attr('tabindex', -1).on('blur focusout', function () {

            // when focus leaves this element, 
            // remove the tabindex attribute
            $(this).removeAttr('tabindex');

        }).focus().trigger('click');
		
		console.log( 'Link skipping to: ' + skip );
		
	});
	
	$('.skip-to-body').on('click', function(evt) {
		
		evt.preventDefault();
		evt.stopPropagation();
		
		$('body > .container').attr('tabindex', -1).on('blur focusout', function() {
			//when focus leaves this element
			//remove the tabindex attribute
			$(this).removeAttr('tabindex');
		}).focus().trigger('click');
		
	});
	
	//The following code attaches a listener to the list to add or
	//remove items from their list using the Spacebar..
	$('#listAll').keyup( function( evt ) {
		if( evt.keyCode === 32 ) {
		    var oListAll = document.getElementById("listAll");
		    var oListSel = document.getElementById("listSel");
		    ListboxTossSelected(oListAll, oListSel);
		    
		}
	});
	
	$('#listSel').keyup( function( evt ) {
		if( evt.keyCode === 32 ) {
		    var oListAll = document.getElementById("listAll");
		    var oListSel = document.getElementById("listSel");
		    ListboxTossSelected(oListSel, oListAll);
		    ListboxRemoveSelected(oListSel);
		}
	});
	//-----------
	
	
	/* @author 	rothrob
	 * @date   	09/24/2014
	 * @desc   	Object that controls the accessibility popup,
	 * 					its children, and behaviors. This makes things
	 *					easier to code, debug, and understand.
	 */
	var AccessibilityDialog =  {
		//This is the actual element itself, in this case this
		//is the dialog div containing the instructions
		element : $('#accessibility_dialog'),
		//This is the skip-link that trigger the opening of the
		//popup element
		trigger: $('#accessibility_open'),
		//This initializes the objects and runs functions it 
		//requires to work properly
		initialize : function() {
			//Build the dialog popup including appending unique
			//instructions on some pages.
			this.build();
			//Render the jQuery UI dialog popup
			this.renderDialog();
			//Attach event listeners to the elements
			this.events();
		},
		
		build: function() {
			
			var body = $('body').attr('class'),
				html = '';
			
			console.log(body);
			
			switch( body ) {
				case 'home-page' :
					html += '<p>Skip to Latest News - allows you to skip to the news section.</p>' +
							'<p>Skip to Main Content - allows you to skip to the welcome section.</p>' +
							'<p> Skip to Local Footer - allows you to skip to the footer section. </p>';
					break;
				case 'summary-page' :
					html += '<p>Skip to State List - allows you to skip to the state dropdown list.</p>' +
							'<p>Skip to Summary Map - allows you to skip to the Summary Map of visible states. </p>' +
							'<p> Skip to Local Footer - allows you to skip to the footer section. </p>' +
							'<p>You can pane the map using the four arrow keys and zoom in and out using the + and - keys.</p>';
					break;
				case 'faq-page' :
					html += '<p>Skip to F.A.Q. - allows you to skip to the Frequently Asked Questions Menu.</p>' +
							'<p>The Frequently Asked Questions Menu is an accordion menu, this means that questions' + 
							'will be collapsed and can be toggle open or close by pressing enter on the category.</p>' +
							'<p> Skip to Local Footer - allows you to skip to the footer section. </p>';
					break;
				case 'wde-map-page':
					html += '<p> Skip to Local Footer - allows you to skip to the footer section. </p>' + 
							'<p>You can pane the map using the four arrow keys and zoom in and out using the + and - keys.</p>';
					break;
			}

			//Append the instructions to the dialog popup
			this.element.append(html);
		},
		// Builds the jQuery UI Dialog object
		renderDialog : function() {
			this.element.dialog({
				autoOpen: false
			});
		},
		// Contains and controlls events
		events : function() {
			// Cache "this" to "self" to access this object in deep callbacks
			// as you know "this" can differ throughout the code.
			var self = this;
			self.trigger.on('click', function(e) {
				e.preventDefault();
				self.element.dialog('open');
			});
		}
	};
	//Initialize Accessibility Dialog popup
	AccessibilityDialog.initialize();
	
})(jQuery);