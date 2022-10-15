(ns heraldicon.frontend.contact
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.title :as title]
   [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [::title/set :string.menu/contact])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 [tr :string.menu/contact]]
   [:p [tr :string.text.contact/feel-free-to-contact-me]]

   [:p {:style {:margin-top "1em"}}
    [tr :string.text.contact/project-info]
    ": "
    [:a {:href "https://github.com/heraldry" :target "_blank"} "github:heraldry"]]

   [:p [tr :string.text.contact/developer-info] ":"]
   [:ul
    [:li [:a {:href "https://www.reddit.com/r/heraldry/" :target "_blank"} "reddit/heraldry"]
     ", " [tr :string.text.contact/my-username-is] " " [:em "tierced"]]
    [:li [:a {:href "https://discord.gg/EGbMW8dth2" :target "_blank"} "discord:Heraldicon"]
     ", " [tr :string.text.contact/my-username-is] " " [:em "or#5915"]]]
   [:iframe
    {:sandbox "allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts",
     :frameBorder "0",
     :allowtransparency "true",
     :height "500",
     :width "350",
     :src "https://discord.com/widget?id=847884812836012072&theme=dark"}]
   [:h2 [tr :string.text.contact/translation]]
   [:p
    [tr :string.text.contact/translation-not-yet-complete]
    " "
    [:a {:href "https://crowdin.com/project/heraldicon/" :target "_blank"}
     [tr :string.text.contact/crowdin-project]]
    "."]
   [:h3 [tr :string.text.contact/translation-credits]]
   [:p [tr :string.text.contact/translation-credits-intro]]
   [:ul
    [:li "Українська: Dughorm"]
    [:li "Italiano: ashoppio#2022 (Brando Maria Pernice)"]]])
