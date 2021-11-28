(ns heraldry.frontend.contact
  (:require
   [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [:set-title "About"])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 "Contact"]
   [:p
    "You can email me at oliver [at] heraldry.digital for feedback, questions, complaints, feature "
    "requests, or deep and meaningful friendship."]

   [:p {:style {:margin-top "1em"}}
    "The project is on " [:a {:href "https://github.com/heraldry" :target "_blank"} "github:heraldry"]
    " if you want to get involved in some way."]

   [:p "I can be found on:"]
   [:ul
    [:li [:a {:href "https://www.reddit.com/r/heraldry/" :target "_blank"} "reddit/heraldry"] ", where my username is " [:em "tierced"]]
    [:li [:a {:href "https://discord.gg/EGbMW8dth2" :target "_blank"} "discord:heraldry.digital"] ", where my username is " [:em "or#5915"]]]
   [:iframe
    {:sandbox "allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts",
     :frameBorder "0",
     :allowtransparency "true",
     :height "500",
     :width "350",
     :src "https://discord.com/widget?id=847884812836012072&theme=dark"}]])
