(ns heraldry.frontend.contact
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.gettext :refer [string]]
   [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [:set-title (string "Infos")])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 [tr (string "Contact")]]
   [:p [tr (string "You can email me at oliver [at] heraldry.digital for feedback, questions, complaints, feature requests, or deep and meaningful friendship.")]]

   [:p {:style {:margin-top "1em"}}
    [tr (string "The project can be found here, if you want to get involved")]
    ": "
    [:a {:href "https://github.com/heraldry" :target "_blank"} "github:heraldry"]]

   [:p [tr (string "I can be found here")] ":"]
   [:ul
    [:li [:a {:href "https://www.reddit.com/r/heraldry/" :target "_blank"} "reddit/heraldry"]
     ", " [tr (string "my username is")] " " [:em "tierced"]]
    [:li [:a {:href "https://discord.gg/EGbMW8dth2" :target "_blank"} "discord:heraldry.digital"]
     ", " [tr (string "my username is")] " " [:em "or#5915"]]]
   [:iframe
    {:sandbox "allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts",
     :frameBorder "0",
     :allowtransparency "true",
     :height "500",
     :width "350",
     :src "https://discord.com/widget?id=847884812836012072&theme=dark"}]])
