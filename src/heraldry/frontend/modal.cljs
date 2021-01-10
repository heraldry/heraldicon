(ns heraldry.frontend.modal
  (:require [re-frame.core :as rf]))

(def db-path
  [:modal])

(defn create [title content]
  (rf/dispatch [:set db-path {:title   title
                              :content content}]))
(defn clear []
  (rf/dispatch [:remove db-path]))

(defn render []
  (let [{:keys [title content]} @(rf/subscribe [:get db-path])]
    (when content
      [:<>
       [:div.modal-background {:on-click #(clear)}]
       [:div.modal
        [:div.header title]
        [:div.content content]]])))
