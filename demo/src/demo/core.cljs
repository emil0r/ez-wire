(ns demo.core
  (:require [clojure.string :as str]
            [demo.common :refer [code]]
            [demo.forms.flight :refer [form-flight]]
            [demo.forms.login :refer [form-login]]
            [demo.forms.order :refer [form-order]]
            [demo.forms.templated :refer [form-templated]]
            [demo.forms.wired :refer [form-wired]]
            [demo.forms.wizard :refer [form-wizard]]
            [reagent.core :as r]
            [reagent.dom :as rdom]))


(defn- create-link [heading]
  (-> heading
      str/lower-case
      str/trim
      (str/replace #"\s" "-")))

(defn- explanation-table [explanations]
  [:table.requirements>tbody
    (for [[k explanation] explanations]
      ^{:key k}
      [:tr [:th (pr-str k)] [:td explanation]])])

(defn explain-form [link]
  [:div.form {:id (create-link link)}
   [:h4.subtitle "Form"]
   [:p "A form is defined using " [:strong "defform"] ", which is a macro imported from " [:strong.ns "ez-wire.form.macros"]]
   [:p "In our namespace we import the " [:strong "defform"] " macro and " [:strong.ns "ez-wire.form"]]
   (code '(ns example
            (:require [ez-wire.form :as form])
            (:require-macros [ez-wire.form.macros :refer [defform]])))
   [:p "We define our form and name it loginform. We have options as the second argument and a list of fields for the third argument. The fields hold a text input and a password input."]
   [:pre>code.clojure "(defform name-of-form 
  options 
  [field1 
   field2 
   field3])"]
   (code '(defform loginform
            {}
            [{:element input-text
              :placeholder "Input your password"
              :name :username}
             {:element input-password
              :placeholder "Input your password"
              :name :password}]))
   [:h5 "Options"]
   [:p "Options is a map that can be used to change default behaviour."]
   (explanation-table [[:label? "Set to false to turn off all labels in a form"]
                       [:template "See options for rendering"]
                       [:wiring "See options for rendering"]
                       [:form/automatic-cleanup? "Set to false to not have automatic clean of the form when a rendered form is destroyed"]
                       [:render [:div "Set to " [:strong ":wizard"] " to render the form as a wizard"]]
                       [:wizard [:div "Map to hold all the steps in the wizard, which fields belongs to each step and allows for an optional legend"
                                 (code '{ ;; static props to apply to the button
                                         :button/props {:type "primary"}
                                         :button/element my-reagent-button
                                         ;; optional css map
                                         :css {}
                                         :steps [{:fields [:username]
                                                  :legend [:h3 "Username"]
                                                  ;; can put in special css in this step
                                                  ;; as well
                                                  :css {}}
                                                 {:fields [:password]
                                                  :legend [:h3 "Password"]}]})]]
                       [:class "CSS classes to be applied to a rendering"]
                       [:style "An inline style to applied to a rendering"]
                       [:list/type "The type of list to render as if using as-list as the rendering method"]])
   
   [:p "We define a Form-2 component that will initialize and render our form."]
   (code '(defn render-my-form []
            (let [form (loginform opts initial-data)]
              (fn []
                [form/as-table {} form]))))
   [:p "An initalized form will hold the following data"]
   (explanation-table [[:id "The ID of the form. By default a UUID is generated."]
                       [:errors "A map with the keys of each field holding a RAtom for the errors per field"]
                       [:data "A RAtom holding the data input for each field"]
                       [:field-ks "A vector with the name of every field"]
                       [:options "The options map"]])

   [:h5 "Rendering a table"]
   [:p "Every render method of a form takes an options map and the form. The options map overrides anything that is present in the " [:strong "defform"] " options map."]
   (explanation-table [['as-list "Renders the form as a list. Defaults to an unordered list. Specify " [:strong ":list/type"] " in options map to change to an ordered list"]
                       ['as-paragraph "Renders the form as a sequence of paragraphs (wrapped in a div)."]
                       ['as-table "Renders the form as a table."]
                       ['as-wire [:div
                                  "Wire lets you define the full rendering of a form. Wire takes a hiccup vector where special syntax marks where parts of an element will render. The syntax is a keyword that looks like this " [:strong ":$name-of-field.part"]
                                  ". The parts available are" [:strong "label, field, errors, text and help"] ". An example wiring could be the first field showing across all available width, with field2 and field3 split across two columns."
                                  (code '{:wiring [:div.my-wrapper-html-element
                                                   [:div
                                                    :$field1.label
                                                    :$field1.field]
                                                   [:div.columns
                                                    [:div.column
                                                     :$field2.field
                                                     :$field2.help]
                                                    [:div.column
                                                     :$field3.field
                                                     :$field3.help]]]})]]
                       ['as-template [:div "Template lets you define the full rendering for every field. This is the same as wire, but only applied for each field. Just like wiring it uses the parts, but does not need to specify the field name. In addition it is advised that " [:strong ":$key"] "is used when using template as a rendering method."
                                      (code '{:template [:div.template :$key
                                                         :$label
                                                         :$field
                                                         :$errors
                                                         :$text
                                                         :$help]})]]])

   [:h5 "Integration with re-frame"]
   [:p "re-frame subscriptions are available for errors, validity and the current wizard step."]
   (explanation-table [[:ez-wire.form/error [:div "Subscription that takes " [:strong.args "[:ez-wire.form/error id-of-form field-name-in-form]"] ". Gives back a list of errors for that field."]]
                       [:ez-wire.form/on-valid [:div "Subscription that takes " [:strong.args "[:ez-wire.form/on-valid id-of-form]"] ". Returns either :ez-wire.form/invalid or the data for all the fields."]]
                       [:ez-wire.form.wizard/current-step [:div "Subscription that takes " [:strong.args "[:ez-wire.form.wizard/current-step id-of-form]"] ". Returns the current step as defined in the wizard setting in the options map for the form."]]])])

(defn explain-fields [link]
  [:div.fields {:id (create-link link)}
   [:h4 "Fields"]
   [:p "A field in a form is a map that holds relevant meta data."]
   [:h5 "Required keys in a field"]
   (explanation-table [[:element "Holds the reagent component that will render the field"]
                       [:name "The name by which the field is referenced"]])
   [:h5 "Optional keys in a field"]
   [:em "NB"]
   [:p "When using " [:strong ":label, :help and :text"] " the value used is sent through the i18n layer. This allows for sending in " [:strong "keywords"] " and have them translated on the fly. By default the i18n implementation returns strings just as they are."]
   
   (explanation-table [[:adapter [:div "Assuming a react component in " [:strong ":element"] " adapter takes the element and returns a reagent component that ez-wire.form can use"]]
                       [:label [:div
                                "Optional usage of a label. The label is set to target the input field of the component given in " [:strong ":element"] ".  If set to " [:strong "true"] " the label will use the " [:strong ":name"] " in the generated HTML label. When set to a value, the value will be used."]]
                       [:text "Will display general info text. Is generated after the field and errors."]
                       [:help "Will display help text. Is generated after the field, errors and general info text."]
                       [:validation "Validation that the field need to run against."]
                       [:id "Give the field an id that is always set, as opposed to it being generated for every new instance of a form"]
                       [:error-element "Override the element that renders errors with your own"]
                       [:value "Give a default value to the field"]
                       [:css [:div "A map that can hold CSS classes for " [:strong ":text, :help, :label and :error"]]]
                       [:wiring "A wiring specific for the field. See rendering forms for more information about wiring."]])

   [:p "Any additional keys in a field map is sent along as is."]
   [:h5 "Keys often used"]
   (explanation-table [[:placeholder "Typically used to hold a placeholder value for input fields"]])
   
   [:h5 "Keys generated by ez-form.wire"]
   (explanation-table [[:model [:div "A reactive model that can be swapped, reset however you wish. " [:strong ":model"] " is an RCursor that is mapped onto the field in the RAtom that is initiated with every new instance of a form. Updating the model, will also update the data RAtom in the form."]]
                       [:error-element [:div "If no error-element is given, the default implementation will be used. Found in " [:strong.ns "ez.wire.form.elements/error-element"]]]])])

(defn explain-validation [link]
  [:div.validation {:id (create-link link)}
   [:h4 "Validation"]
   [:p "Validation is implemented in ez-wire as a protocol in " [:strong.ns "ez-wire.form.protocols"] " and a macro in " [:strong.ns "ez-wire.form.validation"] ". Default protocol implementations are provided for nil, keywords and vectors."]
   (code '(defprotocol IValidate
            :extend-via-metadata true
            (valid? [validation value form] "Evaluate if this validation is true given the value")
            (get-error-message [validation value form] "Get any error messages for this validation")
            (get-affected-fields [validation] "Get any fields that might be affected by this validation. This can include the same field as the validation is attached to.")))
   [:h5 "defvalidation"]
   [:p [:strong "defvalidation"] " is a macro that takes an already existing spec, or a spec key and the spec value, and finally a value that will be used to give back the error. The last value can either be a function, a keyword or something else. If it is a function it'll be run against, passing in one value called the context. If it is not a function, the i18n protocol's t function will be called on it."
    ]
   [:pre>code.clojure
    "(defmacro defvalidation
            ([spec-k t-fn-or-keyword]
             `(swap! errors assoc ~spec-k ~t-fn-or-keyword))
            ([spec-k spec-v t-fn-or-keyword]
             `(do (spec/def ~spec-k ~spec-v)
                  (swap! errors assoc ~spec-k ~t-fn-or-keyword))))"]
   (code '(defvalidation ::my-spec ::my-error-message))
   (code '(defvalidation ::my-spec string? ::my-error-message))
   [:p [:strong "defmultivaldiaton"]" is a macro that takes a name, a set of keys (to be matched against the form the validation is used in), a function and a t-fn-or-keyword value that behaves exactly the same as " [:strong "defvalidation"] "."]
   [:p "The " [:strong "function"] " takes one argument, which is a context map with the "
    [:strong "keys :values and :form"]
    ". The function is expected to return a boolean value, indicating if the validation is valid or not."]
   [:pre>code.clojure
    "(defmacro defmultivalidation
  ([name ks function t-fn-or-keyword]
   `(do (assert (set? ~ks) \"ks need to be a set\")
        (def ~name (map->MultiValidation {:ks ~ks :function ~function}))
        (swap! errors assoc ~name ~t-fn-or-keyword))))
"]
   (code '(defmultivalidation more-than-one
            #{:field1 :field2}
            (fn [context]
              (let [values (:values context)]
               (every? (fn [v] (not (str/blank? v))) (vals values))))
            (fn [_]
              [:div "You need more than one value present"])))
   [:p "nil implements false for " [:strong "valid?"] " and nil for " [:strong "get-error-message"]]
   [:p "keyword implements spec/valid? for " [:strong "valid?"] " and returns the " [:strong "t-fn-or-keyword"] " defined in the " [:strong "defvalidation"] " macro."]
   [:p "vector implements a referral to the protocol, where each element in the vector is checked against the protocol. This holds true for both " [:strong "valid?"] " and " [:strong "get-error-message"]]])

(defn explain-i18n [link]
  [:div.i18n {:id (create-link link)}
   [:h4 "i18n (internationalization)"]
   [:p "i18n is implemented in ez-wire as a protocol in " [:strong.ns "ez-wire.protocols"] ". It implements one method " [:strong "t"] ". It is up to the user of the library to make use of this, in order to adapt whichever i18n library that is used. By default nil, string and keyword are implemented. Examples for how to use i18n with a cljs i18n library are given below."]
   (code '(defprotocol Ii18n
            :extend-via-metadata true
            (t [k] [k args])))])

(defn explain-helper-functions [link]
  [:div.helper-functions {:id (create-link link)}
   [:h4 "Helper functions"]
   [:p "In " [:strong.ns "ez-wire.form"] " you have the following helper functions"]
   (explanation-table [['cleanup-form! "Takes a form as argument. Will manually cleanup the form in re-frame's db"]
                       ['reset-form! "Takes a form as argument. Will reset a form to its initial condition when it was first initialized."]])])

(defn explain-branching [link]
  [:div.branching {:id (create-link link)}
   [:h4 "branching"]
   [:p "Branching is a way for a form in ez-wire.form to branch a form into a tree structure, where each choice in the form, can lead to new choices be made available, or choices to be set in stone."]
   [:p "When declaring a form, or initating a form, you can define a branch in the following way"]
   (code '(defform myform
            {:branch/branching? true
             :branch/branches {:field1 (fn [{:keys [form field-k value]}]
                                         (condp = value
                                           "all"
                                           {:show-fields :all}
                                           "none"
                                           {:hide-fields :all}
                                           "change-other-fields"
                                           {:fields {:dropdown-field2 {:options ["new"
                                                                                 "options"
                                                                                 "to"
                                                                                 "my"
                                                                                 "dropdown"]}}}
                                           "hide-everything"
                                           {:hide-fields :all
                                            :exclude-branching-field? false}

                                           ;; default. always include
                                           {}))}}
            [{:name :field1
              :element input-text}
             {:name :dropdown-field2
              :element input-dropdown
              :options ["my"
                        "options"]}
             {:name :field-i-dont-show
              :active? false
              :element input-text}]))])

(defn concepts []
  [:div.concepts
   [:h2.title "ez-wire.form"]
   [:p.mb-2 "When writing for the web, forms are the bread and butter for gathering information. ez-wire.form handles everything we need for forms in one place."]
   [:p.mb-2 "In a form we define the data we wish to"]
   [:ul.ml-6.has-text-danger
    (for [text ["gather as fields"
                "how to display these fields"
                "what is valid input"
                "show that faulty input is present"
                "handle i18n"
                "gather the data and send it elsewhere"]]
      [:li text])]
   [:h4 "TOC"]
   [:ul
    (for [text ["Explain form"
                "Explain fields"
                "Explain validation"
                "Explain i18n"
                "Explain branching"
                "Helper functions"
                "Login form"
                "Flight form"
                "Wizard flight form"
                "Wired flight form"
                "Templated flight form"
                "Branching order form"]]
      [:li [:a {:href (str "#" (create-link text))} text]])]
   (explain-form "Explain form")
   (explain-fields "Explain fields")
   (explain-validation "Explain validation")
   (explain-i18n "Explain i18n")
   (explain-branching "Explain branching")
   (explain-helper-functions "Helper functions")])

(defn info []
  [:div.info
   [:div.columns>div.column
    [:h1.title "ez-wire"]
    [:img.logo {:src "img/logo.png"}]
    [:p.mt-5
     "Repo found at " [:a {:href "https://github.com/emil0r/ez-wire"} "GitHub"]]
    [:p "ez-wire is written for " [:a {:href "https://reagent-project.github.io/"} "reagent"] " and " [:a {:href "https://day8.github.io/re-frame/"} "re-frame."]]
    [:p "Both are excellent projects, and bring a lot of value as building blocks. ez-wire is yet another building block, that sits above these two more foundational blocks."]]])

(defn index []
  [:div.container.mt-6
   [info]
   [:div.columns>div.column
    
    [concepts]    
    [:ul.mt-6
     (for [[heading comp] [["Login form" form-login]
                           ["Flight form" form-flight]
                           ["Wizard flight form" form-wizard]
                           ["Wired flight form" form-wired]
                           ["Templated flight form" form-templated]
                           ["Branching order form" form-order]]]
       (let [link (create-link heading)]
         [:li {:id link :key link}
          [:h3.subtitle heading]
          [comp]]))]]])


(defn ^:export init []
  (rdom/render [index]
               (.getElementById js/document "app")))


(defn start []
  (init))
