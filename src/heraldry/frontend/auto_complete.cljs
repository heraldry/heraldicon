(ns heraldry.frontend.auto-complete
  (:require
   [re-frame.core :as rf]))

(def db-path
  [:ui :auto-complete])

(defn set-choices [choices]
  (rf/dispatch [:set (conj db-path :choices) choices]))

(defn set-position [position]
  (rf/dispatch [:set (conj db-path :position) position]))

(defn set-data [data]
  (rf/dispatch [:set db-path data]))

(rf/dispatch [:remove db-path])

(defn render []
  (let [{:keys [choices position]} @(rf/subscribe [:get db-path])]
    (when (and position
               (seq choices))
      (into [:ul.auto-complete-box {:style {:left (-> position
                                                      :left
                                                      (str "px"))
                                            :top (str "calc(" (:top position) "px + 5px + 1em)")}}]
            (map (fn [choice]
                   [:li choice]))
            choices))))
