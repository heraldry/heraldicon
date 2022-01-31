(ns heraldry.frontend.preview
  (:require
   [clojure.string :as s]
   [heraldry.config :as config]
   [re-frame.core :as rf]))

(defn effective-version [data]
  (let [version (:version data)]
    (if (zero? version)
      (or (:latest-version data) 0)
      version)))

(defn preview-url [kind {:keys [id] :as arms} & {:keys [width height]}]
  (let [url (or (config/get :heraldry-site-url)
                (config/get :heraldry-url))]
    (str url "/preview/" (name kind) "/" (-> id (s/split #":") last) "/" (effective-version arms) "/preview.png"
         (when (and width height)
           (str "?width=" width "&height=" height)))))

(defn preview-image [kind item]
  (let [url (preview-url kind item :width 300 :height 220)
        loaded-flag-path [:ui :preview-image-loaded? url]
        loaded? @(rf/subscribe [:get loaded-flag-path])]
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
