(ns heraldicon.frontend.preview
  (:require
   [clojure.string :as s]
   [heraldicon.config :as config]
   [re-frame.core :as rf]))

(defn preview-url [kind {:keys [id version]} & {:keys [width height]}]
  (let [url (or (config/get :heraldicon-site-url)
                (config/get :heraldicon-url))]
    (str url "/preview/" (name kind) "/" (-> id (s/split #":") last) "/" (or version 0) "/preview.png"
         (when (and width height)
           (str "?width=" width "&height=" height)))))

(defn preview-image [kind item]
  (let [url (preview-url kind item :width 300 :height 215)
        loaded-flag-path [:ui :preview-image-loaded? url]
        loaded? (or @(rf/subscribe [:get loaded-flag-path])
                    @(rf/subscribe [:get [:ui :crawler?]]))]
    [:<>
     [:img {:src url
            :on-load (when-not loaded? #(rf/dispatch [:set loaded-flag-path true]))
            :style {:display (if loaded? "block" "none")
                    :position "absolute"
                    :margin "auto"
                    :top 0
                    :left 0
                    :right 0
                    :bottom 0
                    :max-width "100%"
                    :max-height "100%"}}]
     (when-not loaded?
       [:div.loader {:style {:font-size "0.5em"
                             :position "absolute"
                             :margin "auto"
                             :top 0
                             :left 0
                             :right 0
                             :bottom 0}}])]))
