(ns heraldicon.frontend.element.search-field
  (:require
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(rf/reg-event-fx ::on-change
  (fn [_ [_ context value on-change]]
    (if on-change
      (do
        (js/setTimeout #(on-change value) 0)
        nil)
      {:dispatch [:set context value]})))

(rf/reg-event-fx ::update-search-field
  (fn [_ [_ context value on-change]]
    {::debounce/dispatch [::debounce-update-search-field [::on-change context value on-change] 250]}))

(defn search-field [context & _]
  (let [current-value (interface/get-raw-data context)
        tmp-value (r/atom current-value)]
    (fn [context & {:keys [on-change]}]
      [:div.search-field
       [:i.fas.fa-search]
       [:input {:name "search"
                :type "search"
                :value @tmp-value
                :autoComplete "off"
                :on-change #(let [value (-> % .-target .-value)]
                              (reset! tmp-value value)
                              (rf/dispatch [::update-search-field context @tmp-value on-change]))
                :style {:outline "none"
                        :border "0"
                        :margin-left "0.5em"
                        :width "calc(100% - 12px - 1.5em)"}}]])))
