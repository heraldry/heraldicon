(ns heraldry.frontend.ui.element.tincture-modifiers
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.ui.element.checkbox :as checkbox]
            [heraldry.frontend.ui.element.range :as range]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.tincture-select :as tincture-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn tincture-modifiers-submenu [path]
  (when-let [variant @(rf/subscribe [:get-value (-> path
                                                    drop-last
                                                    vec
                                                    (conj :variant))])]
    (let [options @(rf/subscribe [:get-relevant-options path])
          {:keys [ui]} options
          label (:label ui)
          tincture-data @(rf/subscribe [:get-value path])
          charge-data (charge/fetch-charge-data variant)
          supported-tinctures (-> attributes/tincture-modifier-map
                                  keys
                                  set
                                  (conj :secondary)
                                  (conj :tertiary)
                                  (conj :shadow)
                                  (conj :highlight)
                                  (set/intersection
                                   (-> charge-data
                                       :colours
                                       (->> (map second))
                                       set)))
          sorted-supported-tinctures (-> supported-tinctures
                                         (disj :shadow)
                                         (disj :highlight)
                                         sort
                                         vec)
          tinctures-set (-> tincture-data
                            (->> (filter (fn [[_ v]]
                                           (and (some? v)
                                                (not= v :none))))
                                 (map first)
                                 set)
                            (filter supported-tinctures)
                            (->> (map util/translate-cap-first)))
          tinctures-title (if (-> tinctures-set count pos?)
                            (-> (util/combine ", " tinctures-set)
                                s/lower-case
                                util/upper-case-first)
                            "None")
          link-name (if (-> tinctures-title count (> 30))
                      (str (subs tinctures-title 0 27) "...")
                      tinctures-title)]
      (when options
        [:div.ui-setting
         (when label
           [:label label])
         [:div.option
          (if (empty? supported-tinctures)
            [:span.disabled "not supported by charge"]
            [submenu/submenu path label link-name {:width "30em"}
             [:div.placeholders
              (when (get supported-tinctures :shadow)
                [range/range-input
                 (conj path :shadow)])
              (when (get supported-tinctures :highlight)
                [range/range-input
                 (conj path :highlight)])
              (for [t sorted-supported-tinctures]
                ^{:key t}
                [tincture-select/tincture-select
                 (conj path t)
                 :option {:type :choice
                          :choices tincture/choices
                          :default :none
                          :ui {:label (util/translate-cap-first t)}}])]])]]))))

(defmethod interface/form-element :tincture-modifiers [path]
  [tincture-modifiers-submenu path])
