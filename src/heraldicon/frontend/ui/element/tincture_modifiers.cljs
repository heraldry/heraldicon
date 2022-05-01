(ns heraldicon.frontend.ui.element.tincture-modifiers
  (:require
   [clojure.set :as set]
   [clojure.string :as s]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.context :as c]
   [heraldicon.frontend.charge :as charge]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.range :as range]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.element.tincture-select :as tincture-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.util :as util]))

(defn tincture-modifiers-submenu [context]
  (let [preview? (interface/get-raw-data (-> context c/-- (c/++ :preview?)))
        variant (interface/get-raw-data (-> context c/-- (c/++ :variant)))]
    (when (or preview?
              variant)
      (let [options (interface/get-relevant-options context)
            {:keys [ui]} options
            label (:label ui)
            tincture-data (interface/get-raw-data context)
            sanitized-tincture-data (merge tincture-data
                                           (options/sanitize tincture-data options))
            charge-data (when (not preview?)
                          (charge/fetch-charge-data variant))
            qualifiers (->> charge-data
                            :colours
                            (map second)
                            (map attributes/tincture-modifier-qualifier)
                            set)
            shadow-qualifiers? (->> qualifiers
                                    (keep attributes/shadow-qualifiers)
                                    first)
            highlight-qualifiers? (->> qualifiers
                                       (keep attributes/highlight-qualifiers)
                                       first)
            supported-tinctures (-> attributes/tincture-modifier-map
                                    keys
                                    set
                                    (conj :secondary)
                                    (conj :tertiary)
                                    (conj :shadow)
                                    (conj :highlight)
                                    (cond->
                                      (not preview?) (set/intersection
                                                      (->> charge-data
                                                           :colours
                                                           (map second)
                                                           (map attributes/tincture-modifier)
                                                           set))
                                      shadow-qualifiers? (conj :shadow)
                                      highlight-qualifiers? (conj :highlight)))
            sorted-supported-tinctures (-> supported-tinctures
                                           (disj :shadow)
                                           (disj :highlight)
                                           sort
                                           vec)
            tinctures-set (-> sanitized-tincture-data
                              (->> (filter (fn [[_ v]]
                                             (and (some? v)
                                                  (not= v :none))))
                                   (map first)
                                   set)
                              (disj :shadow)
                              (disj :highlight)
                              (filter supported-tinctures)
                              sort
                              vec
                              (cond->
                                (and (:shadow supported-tinctures)
                                     (-> sanitized-tincture-data
                                         :shadow
                                         pos?)) (conj :shadow)
                                (and (:highlight supported-tinctures)
                                     (-> sanitized-tincture-data
                                         :highlight
                                         pos?)) (conj :highlight)))
            tinctures-title (if (-> tinctures-set count pos?)
                              (->> tinctures-set
                                   (map util/translate)
                                   (util/combine ", ")
                                   s/lower-case
                                   util/upper-case-first)
                              :string.charge.tincture-modifier/none)
            link-name (if (and (string? tinctures-title)
                               (-> tinctures-title count (> 30)))
                        (str (subs tinctures-title 0 27) "...")
                        tinctures-title)]
        (when options
          [:div.ui-setting
           (when label
             [:label [tr label]])
           [:div.option
            (if (empty? supported-tinctures)
              [:span.disabled "not supported by charge"]
              [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                             :class "submenu-tincture-modifiers"}
               [:div.placeholders
                (when (get supported-tinctures :shadow)
                  [range/range-input
                   (c/++ context :shadow)])
                (when (get supported-tinctures :highlight)
                  [range/range-input
                   (c/++ context :highlight)])
                (for [t sorted-supported-tinctures]
                  ^{:key t}
                  [tincture-select/tincture-select
                   (c/++ context t)
                   ;; TODO: this could probably be generated dynamically in charge/options
                   :default-option {:type :choice
                                    :choices tincture/choices
                                    :default :none
                                    :ui {:label (util/translate-cap-first t)}}])]])]])))))

(defmethod ui.interface/form-element :tincture-modifiers [context]
  [tincture-modifiers-submenu context])
