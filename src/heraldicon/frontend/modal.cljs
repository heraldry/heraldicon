(ns heraldicon.frontend.modal
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [re-frame.core :as rf]))

(def ^:private dialog-db-path
  [:modal :dialog])

(def ^:private loader-db-path
  [:modal :loader])

(defn create [title content & {:keys [on-cancel]}]
  (rf/dispatch [:set dialog-db-path {:title title
                                     :content content
                                     :on-cancel on-cancel}]))
(rf/reg-event-db ::create
  (fn [db [_ title content on-cancel]]
    (assoc-in db dialog-db-path {:title title
                                 :content content
                                 :on-cancel on-cancel})))

(rf/reg-event-db ::clear
  (fn [db _]
    (when-let [on-cancel (get-in db (conj dialog-db-path :on-cancel))]
      (on-cancel))
    (assoc-in db dialog-db-path nil)))

(defn clear []
  (rf/dispatch [::clear]))

(defn start-loading []
  (rf/dispatch [:set loader-db-path true]))

(defn stop-loading []
  (rf/dispatch [:set loader-db-path nil]))

(defn render []
  (let [{:keys [title content]} @(rf/subscribe [:get dialog-db-path])
        loader @(rf/subscribe [:get loader-db-path])]
    ^{:key title}
    [:<>
     (when content
       [:<>
        [:div.modal-background {:on-click #(clear)}]
        [:div.modal.dialog
         [:div.modal-header (if (keyword? title)
                              [tr title]
                              title)]
         [:div.modal-content content]]])
     (when loader
       [:<>
        [:div.modal-background {:style {:z-index 2000}}]
        [:div.modal {:style {:z-index 2001}}
         [:div.loader]]])]))
