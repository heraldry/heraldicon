(ns heraldry.frontend.form.field
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.division.core :as division]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.charge :as charge]
            [heraldry.frontend.form.division :as division-form]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.geometry :as geometry]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.ordinary :as ordinary]
            [heraldry.frontend.form.position :as position]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn form-for-content [path]
  [:div.form-content
   [tincture/form (conj path :tincture)]])

(defn form-for-layout [field-path & {:keys [title options] :or {title "Layout"}}]
  (let [layout-path (conj field-path :division :layout)
        division @(rf/subscribe [:get (conj field-path :division)])
        layout (:layout division)
        division-type (:type division)
        current-data (:layout (options/sanitize-or-nil division (division-options/options division)))
        effective-data (:layout (options/sanitize division (division-options/options division)))
        link-name (util/combine
                   ", "
                   [(cond
                      (= division-type :paly) (str (:num-fields-x effective-data) " fields")
                      (#{:barry
                         :bendy
                         :bendy-sinister} division-type) (str (:num-fields-y effective-data) " fields"))
                    (when (and (:num-base-fields current-data)
                               (not= (:num-base-fields current-data) 2))
                      (str (:num-base-fields effective-data) " base fields"))
                    (when (or (:offset-x current-data)
                              (:offset-y current-data))
                      (str "shifted"))
                    (when (or (:stretch-x current-data)
                              (:stretch-y current-data))
                      (str "stretched"))
                    (when (:rotation current-data)
                      (str "rotated"))])
        link-name (if (-> link-name count (= 0))
                    "Default"
                    link-name)]
    [:div.setting
     [:label title]
     " "
     [element/submenu layout-path title link-name {}
      (when (-> options :num-base-fields)
        [element/range-input-with-checkbox (conj layout-path :num-base-fields) "Base fields"
         (-> options :num-base-fields :min)
         (-> options :num-base-fields :max)
         :default (options/get-value (:num-base-fields layout) (:num-base-fields options))
         :on-change (fn [value]
                      (rf/dispatch [:set-division-type
                                    field-path
                                    division-type
                                    (:num-fields-x layout)
                                    (:num-fields-y layout)
                                    value]))])
      (when (-> options :num-fields-x)
        [element/range-input-with-checkbox (conj layout-path :num-fields-x) "x-Subfields"
         (-> options :num-fields-x :min)
         (-> options :num-fields-x :max)
         :default (options/get-value (:num-fields-x layout) (:num-fields-x options))
         :on-change (fn [value]
                      (rf/dispatch [:set-division-type
                                    field-path
                                    division-type
                                    value
                                    (:num-fields-y layout)
                                    (:num-base-fields layout)]))])
      (when (-> options :num-fields-y)
        [element/range-input-with-checkbox (conj layout-path :num-fields-y) "y-Subfields"
         (-> options :num-fields-y :min)
         (-> options :num-fields-y :max)
         :default (options/get-value (:num-fields-y layout) (:num-fields-y options))
         :on-change (fn [value]
                      (rf/dispatch [:set-division-type
                                    field-path
                                    division-type
                                    (:num-fields-x layout)
                                    value
                                    (:num-base-fields layout)]))])
      (when (-> options :offset-x)
        [element/range-input-with-checkbox (conj layout-path :offset-x) "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :step 0.01
         :default (options/get-value (:offset-x layout) (:offset-x options))])
      (when (-> options :offset-y)
        [element/range-input-with-checkbox (conj layout-path :offset-y) "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :step 0.01
         :default (options/get-value (:offset-y layout) (:offset-y options))])
      (when (-> options :stretch-x)
        [element/range-input-with-checkbox (conj layout-path :stretch-x) "Stretch x"
         (-> options :stretch-x :min)
         (-> options :stretch-x :max)
         :step 0.01
         :default (options/get-value (:stretch-x layout) (:stretch-x options))])
      (when (-> options :stretch-y)
        [element/range-input-with-checkbox (conj layout-path :stretch-y) "Stretch y"
         (-> options :stretch-y :min)
         (-> options :stretch-y :max)
         :step 0.01
         :default (options/get-value (:stretch-y layout) (:stretch-y options))])
      (when (-> options :rotation)
        [element/range-input-with-checkbox (conj layout-path :rotation) "Rotation"
         (-> options :rotation :min)
         (-> options :rotation :max)
         :step 5
         :default (options/get-value (:rotation layout) (:rotation options))])]]))

(defn form [path & {:keys [parent-field title-prefix]}]
  (let [division (-> @(rf/subscribe [:get path])
                     :division)
        division-type (-> division
                          :type
                          (or :none))
        field @(rf/subscribe [:get path])
        counterchanged? (and @(rf/subscribe [:get (conj path :counterchanged?)])
                             (division/counterchangable? (-> parent-field :division)))
        root-field? (= path [:coat-of-arms :field])]
    [element/component path :field (cond
                                     (:counterchanged? field) "Counterchanged"
                                     (= division-type :none) (-> field :content :tincture util/translate-tincture util/upper-case-first)
                                     :else (-> division-type util/translate-cap-first)) title-prefix
     [:div.settings
      (when (not root-field?)
        [element/checkbox (conj path :inherit-environment?) "Inherit environment (dimidiation)"])
      (when (and (not= path [:coat-of-arms :field])
                 parent-field)
        [element/checkbox (conj path :counterchanged?) "Counterchanged"
         :disabled? (not (division/counterchangable? (-> parent-field :division)))])
      (when (not counterchanged?)
        [:<>
         [division-form/form path]
         (let [division-options (division-options/options (:division field))]
           [:<>
            (when (:line division-options)
              [line/form (conj path :division :line) :options (:line division-options)])
            (when (:opposite-line division-options)
              [line/form (conj path :division :opposite-line)
               :options (:opposite-line division-options)
               :defaults (options/sanitize (:line division) (:line division-options))
               :title "Opposite Line"])
            (when (:extra-line division-options)
              [line/form (conj path :division :extra-line)
               :options (:extra-line division-options)
               :defaults (options/sanitize (:line division) (:line division-options))
               :title "Extra Line"])
            (when (-> division-options :variant)
              [element/select (conj path :division :variant) "Variant"
               (-> division-options :variant :choices)
               :default (-> division-options :variant :default)])
            (when (-> division-options :thickness)
              [element/range-input-with-checkbox (conj path :division :thickness) "Thickness"
               (-> division-options :thickness :min)
               (-> division-options :thickness :max)
               :step 0.01
               :default (-> division-options :thickness :default)])
            (when (-> division-options :origin)
              [position/form (conj path :division :origin)
               :title "Origin"
               :options (:origin division-options)])
            (when (-> division-options :anchor)
              [position/form (conj path :division :anchor)
               :title "Anchor"
               :options (:anchor division-options)])
            (when (:geometry division-options)
              [geometry/form (conj path :division :geometry)
               (:geometry division-options)
               :current (:geometry division)])
            (when (:layout division-options)
              [form-for-layout path :options (:layout division-options)])])
         (if (= division-type :none)
           [form-for-content (conj path :content)]
           [element/checkbox (conj path :division :hints :outline?) "Outline"])])]
     (cond
       (#{:chequy
          :lozengy
          :vairy
          :potenty
          :papellony
          :masonry} division-type) [:div.parts.components {:style {:margin-bottom "0.5em"}}
                                    [:ul
                                     (let [tinctures @(rf/subscribe [:get (conj path :division :fields)])]
                                       (for [idx (range (count tinctures))]
                                         ^{:key idx}
                                         [:li
                                          [tincture/form (conj path :division :fields idx :content :tincture)
                                           :label (str "Tincture " (inc idx))]]))]]
       (not counterchanged?) [:div.parts.components
                              [:ul
                               (let [content @(rf/subscribe [:get (conj path :division :fields)])
                                     mandatory-part-count (division/mandatory-part-count division)]
                                 (for [[idx part] (map-indexed vector content)]
                                   (let [part-path (conj path :division :fields idx)
                                         part-name (division/part-name division-type idx)
                                         ref (:ref part)]
                                     ^{:key idx}
                                     [:li
                                      [:div
                                       (if ref
                                         [element/component part-path :ref (str "Same as " (division/part-name division-type ref)) part-name]
                                         [form part-path :title-prefix part-name])]
                                      [:div {:style {:padding-left "10px"}}
                                       (if ref
                                         [:a {:on-click #(do (state/dispatch-on-event % [:set part-path (get content ref)])
                                                             (state/dispatch-on-event % [:ui-component-open part-path]))}
                                          [:i.far.fa-edit]]
                                         (when (>= idx mandatory-part-count)
                                           [:a {:on-click #(state/dispatch-on-event % [:set (conj part-path :ref)
                                                                                       (-> (division/default-fields division)
                                                                                           (get idx)
                                                                                           :ref)])}
                                            [:i.far.fa-times-circle]]))]])))]])
     [:div {:style {:margin-bottom "0.5em"}}
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/ordinary])}
       [:i.fas.fa-plus] " Add ordinary"]
      " "
      [:button {:on-click #(state/dispatch-on-event % [:add-component path default/charge])}
       [:i.fas.fa-plus] " Add charge"]]
     [:div.components
      [:ul
       (let [components @(rf/subscribe [:get (conj path :components)])]
         (for [[idx component] (reverse (map-indexed vector components))]
           (let [component-path (conj path :components idx)]
             ^{:key idx}
             [:li
              [:div {:style {:padding-right "10px"
                             :white-space "nowrap"}}
               [:a (if (zero? idx)
                     {:class "disabled"}
                     {:on-click #(state/dispatch-on-event % [:move-component-down component-path])})
                [:i.fas.fa-chevron-down]]
               " "
               [:a (if (= idx (dec (count components)))
                     {:class "disabled"}
                     {:on-click #(state/dispatch-on-event % [:move-component-up component-path])})
                [:i.fas.fa-chevron-up]]]
              [:div
               (if (-> component :component (= :ordinary))
                 [ordinary/form component-path
                  :parent-field field
                  :form-for-field form]
                 [charge/form component-path
                  :parent-field field
                  :form-for-field form])]
              [:div {:style {:padding-left "10px"}}
               (when (not (and (-> component :component (= :charge))
                               (-> component :type keyword? not)))
                 [:a {:on-click #(state/dispatch-on-event % [:remove-component component-path])}
                  [:i.far.fa-trash-alt]])]])))]]]))
