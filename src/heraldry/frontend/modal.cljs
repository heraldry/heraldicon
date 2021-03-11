(ns heraldry.frontend.modal
  (:require [re-frame.core :as rf]))

(def dialog-db-path
  [:modal :dialog])

(def loader-db-path
  [:modal :loader])

(defn create [title content & {:keys [on-cancel]}]
  (rf/dispatch [:set dialog-db-path {:title title
                                     :content content
                                     :on-cancel on-cancel}]))
(defn clear []
  (when-let [on-cancel @(rf/subscribe [:get (conj dialog-db-path :on-cancel)])]
    (on-cancel))
  (rf/dispatch [:remove dialog-db-path]))

(defn start-loading []
  (rf/dispatch-sync [:set loader-db-path true]))

(defn stop-loading []
  (rf/dispatch-sync [:set loader-db-path nil]))

(defn render []
  (let [{:keys [title content]} @(rf/subscribe [:get dialog-db-path])
        loader @(rf/subscribe [:get loader-db-path])]
    [:<>
     (when content
       [:<>
        [:div.modal-background {:on-click #(clear)}]
        [:div.modal.dialog
         [:div.header title]
         [:div.content content]]])
     (when loader
       [:<>
        [:div.modal-background {:style {:z-index 2000}}]
        [:div.modal {:style {:z-index 2001}}
         [:div.loader]]])]))
