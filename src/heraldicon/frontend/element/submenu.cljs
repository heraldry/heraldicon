(ns heraldicon.frontend.element.submenu
  (:require
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(def open?-path
  [:ui :submenu-open?])

(rf/reg-sub ::open?
  (fn [db [_ path]]
    (get-in db (conj open?-path path))))

(macros/reg-event-db ::close-all
  (fn [db _]
    (assoc-in db open?-path nil)))

(macros/reg-event-db ::open
  (fn [db [_ path]]
    (-> db
        (update-in open?-path
                   (fn [open-flags]
                     (into {}
                           (keep (fn [[key value]]
                                   (when (or (= key
                                                (take (count key) path))
                                             ;; special case, because these paths are not parents
                                             ;; of the submenu context that spawns them
                                             (some #{:fess-group
                                                     :bend-group
                                                     :pale-group
                                                     :chevron-group} path))
                                     [key value])))
                           open-flags)))
        (assoc-in (conj open?-path path) true))))

(macros/reg-event-db ::close
  (fn [db [_ path]]
    (assoc-in db (conj open?-path path) false)))

(defn submenu [{:keys [path]} title link-name extra & content]
  (let [submenu-id path
        submenu-open? @(rf/subscribe [::open? submenu-id])]
    [:div.ui-submenu-setting {:style {:display "inline-block"}
                              :on-click #(.stopPropagation %)}
     (when submenu-open?
       [:div.ui-component.ui-submenu extra
        [:div.ui-component-header [:a {:on-click (js-event/handled #(rf/dispatch [::close submenu-id]))}
                                   [:i.far.fa-times-circle]]
         " " [tr title]]
        (into [:div.content]
              content)])
     [:a {:on-click (js-event/handled #(rf/dispatch [::open submenu-id]))}
      link-name]]))
