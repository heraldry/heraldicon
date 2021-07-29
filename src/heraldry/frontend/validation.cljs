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

(rf/reg-sub :field-tinctures-for-validation
  (fn [[_ path] _]
    [(rf/subscribe [:get-sanitized-data (conj path :tincture)])
     (rf/subscribe [:get-sanitized-data (conj path :fields 0 :tincture)])
     (rf/subscribe [:get-sanitized-data (conj path :fields 1 :tincture)])])

  (fn [[tincture
        subfield-1-tincture
        subfield-2-tincture] [_ _path]]
    (-> #{tincture}
        (conj subfield-1-tincture)
        (conj subfield-2-tincture)
        (cond->
         ;; at least one of the subfields is not a plain field, but we'll stop here, only
         ;; report that there's more with the magical :mixed tincture
         (and (not tincture)
              (or (not subfield-1-tincture)
                  (not subfield-2-tincture))) (conj :mixed)))))

(rf/reg-sub :fimbriation-tinctures-for-validation
  (fn [[_ path] _]
    [(rf/subscribe [:get-data (conj path :tincture-1)])
     (rf/subscribe [:get-data (conj path :tincture-2)])])

  (fn [[tincture-1 tincture-2] [_ _path]]
    [tincture-1 tincture-2]))

(rf/reg-sub :validate-ordinary
  (fn [[_ path] _]
    (let [parent-field-path (->> path
                                 (drop-last 2)
                                 vec)
          own-field-path (conj path :field)]
      [(rf/subscribe [:field-tinctures-for-validation parent-field-path])
       (rf/subscribe [:field-tinctures-for-validation own-field-path])
       (rf/subscribe [:fimbriation-tinctures-for-validation (conj own-field-path :fimbriation)])
       (rf/subscribe [:fimbriation-tinctures-for-validation (conj own-field-path :line :fimbriation)])
       (rf/subscribe [:fimbriation-tinctures-for-validation (conj own-field-path :opposite-line :fimbriation)])
       (rf/subscribe [:fimbriation-tinctures-for-validation (conj own-field-path :extra-line :fimbriation)])]))

  (fn [[parent-field-tinctures
        own-field-tinctures
        fimbriation-tinctures
        line-fimbriation-tinctures
        opposite-line-fimbriation-tinctures
        extra-line-fimbriation-tinctures] [_ _path]]

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
