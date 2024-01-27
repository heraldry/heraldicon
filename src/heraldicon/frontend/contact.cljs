(ns heraldicon.frontend.contact
  (:require
   [heraldicon.config :as config]
   [heraldicon.frontend.ko-fi :as ko-fi]
   [heraldicon.frontend.title :as title]
   [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [::title/set :string.menu/contact])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:div {:style {:padding "10px"
                  :float "left"
                  :text-align "justify"
                  :min-width "13em"
                  :max-width "28em"}}
    [:h2 "Contact"]
    [:p
     "Email me for feature requests, bug reports, feedback, or questions: "
     (let [email-address (config/get :email-address)]
       [:a {:href (str "mailto:" email-address)} email-address])]

    [:p {:style {:margin-top "1em"}}
     "The project can be found here: "
     [:a {:href "https://github.com/heraldry" :target "_blank"} "github:heraldry"]]

    [:p "I can be found here:"]
    [:ul
     [:li [:a {:href "https://discord.gg/EGbMW8dth2" :target "_blank"} "discord:Heraldicon"]
      ", my username is " [:em "or#5915"]]
     [:li [:a {:href "https://www.reddit.com/r/heraldry/" :target "_blank"} "reddit/heraldry"]
      ", my username is " [:em "tierced"]]]
    [:iframe
     {:sandbox "allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts",
      :frameBorder "0",
      :allowtransparency "true",
      :height "500",
      :width "350",
      :src "https://discord.com/widget?id=847884812836012072&theme=dark"}]
    [:h2 "Translation"]
    [:p
     "Not all strings have been translated yet. If you want to help out or correct something, then reach out on Discord or the "
     [:a {:href "https://crowdin.com/project/heraldicon/" :target "_blank"}
      "Crowdin-Project"]
     "."]
    [:h3 "Credits"]
    [:p "Thanks to everybody helping out with the translations!"]
    [:ul
     [:li "Українська: Dughorm"]
     [:li
      "Italiano: "
      [:a {:href "https://commons.wikimedia.org/wiki/User:Ashoppio"
           :target "_blank"}
       "ashoppio#2022"]]
     [:li "Deutsch (support): "
      [:a {:href "https://github.com/NervousNullPtr"
           :target "_blank"}
       "NervousNullPtr"]]]]
   [:div {:style {:padding "10px"
                  :float "right"
                  :text-align "justify"
                  :min-width "13em"
                  :max-width "28em"}}
    [:h2 "Support"]
    [ko-fi/large-button]
    [:p "I develop and maintain Heraldicon on my own, and I'd like to keep it open, ad-free, and free to use."]
    [:p "As the site grows, the costs for hosting grow with it. So if you enjoy Heraldicon and want to support it, then any tip will be cherished, as it helps keeping the site running."]
    [:p "That being said, please only tip if you can easily spare it; I'm financially secure, and your support is appreciated at any level, so there's no pressure to contribute more than you're comfortable with."]
    [:p "Thanks!"]
    [ko-fi/qr-code]]])
