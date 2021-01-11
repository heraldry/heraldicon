(ns heraldry.frontend.modal
  (:require [re-frame.core :as rf]))

(def db-path
  [:modal])

(defn create [title content & {:keys [on-cancel]}]
  (rf/dispatch [:set db-path {:title title
                              :content content
                              :on-cancel on-cancel}]))
(defn clear []
  (when-let [on-cancel @(rf/subscribe [:get (conj db-path :on-cancel)])]
    (on-cancel))
  (rf/dispatch [:remove db-path]))

(defn render []
  (let [{:keys [title content]} @(rf/subscribe [:get db-path])]
    (when content
      [:<>
       [:div.modal-background {:on-click #(clear)}]
       [:div.modal
        [:div.header title]
        [:div.content content]]])))
