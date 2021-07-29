(ns heraldry.frontend.validation
  (:require [re-frame.core :as rf]))

(def severities
  {:note 1
   :warning 2
   :error 3})

(defn validation-color [level]
  (case level
    :note "#ffd24d"
    :warning "#ffb366"
    :error "#b30000"
    "#ccc"))

(rf/reg-sub :validate-ordinary
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (->> [{:level :error
           :message "An error"}
          {:level :warning
           :message "A warning"}
          {:level :note
           :message "A note"}]
         (sort-by (comp severities :level))
         reverse)))

(defn render-icon [level]
  [:i.fas.fa-exclamation-triangle {:style {:color (validation-color level)}}])

(defn render [validation]
  (if (seq validation)
    (let [first-message (first validation)]
      [:div.tooltip.info {:style {:display "inline-block"
                                  :margin-left "0.2em"}}
       [render-icon (:level first-message)]
       [:div.bottom {:style {:width "25em"}}
        [:ul {:style {:position "relative"
                      :padding-left "1.8em"}}
         (doall
          (for [{:keys [level message]} validation]
            ^{:key message}
            [:li [:div {:style {:position "absolute"
                                :left "0em"}}
                  [render-icon level]]
             message]))]]])
    [:<>]))
