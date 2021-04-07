(ns heraldry.frontend.form.field
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.core :as division]
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

(defn form-for-plain-field [path]
  [:div.form-plain-field
   [tincture/form (conj path :tincture)]])

(defn form-for-layout [field-path & {:keys [title options] :or {title "Layout"}}]
  (let [layout-path (conj field-path :layout)
        field @(rf/subscribe [:get field-path])
        layout (:layout field)
        division-type (:type field)
        field (:layout (options/sanitize-or-nil field (division-options/options field)))
        effective-data (:layout (options/sanitize field (division-options/options field)))
        link-name (util/combine
                   ", "
                   [(cond
                      (= division-type :paly) (str (:num-fields-x effective-data) " fields")
                      (#{:barry
                         :bendy
                         :bendy-sinister} division-type) (str (:num-fields-y effective-data) " fields"))
                    (when (and (:num-base-fields field)
                               (not= (:num-base-fields field) 2))
                      (str (:num-base-fields effective-data) " base fields"))
                    (when (or (:offset-x field)
                              (:offset-y field))
                      (str "shifted"))
                    (when (or (:stretch-x field)
                              (:stretch-y field))
                      (str "stretched"))
                    (when (:rotation field)
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
  (let [field @(rf/subscribe [:get path])
        division-type (:type field)
        counterchanged? @(rf/subscribe [:get (conj path :counterchanged?)])
        root-field? (= path [:coat-of-arms :field])]
    [element/component path :field (cond
                                     (:counterchanged? field) "Counterchanged"
                                     (= division-type :plain) (-> field :tincture util/translate-tincture util/upper-case-first)
                                     :else (-> division-type util/translate-cap-first)) title-prefix
     [:div.settings
      (when (not root-field?)
        [element/checkbox (conj path :inherit-environment?) "Inherit environment (dimidiation)"])
      (when (and (not= path [:coat-of-arms :field])
                 parent-field)
        [element/checkbox (conj path :counterchanged?) "Counterchanged"])
      (when (not counterchanged?)
        [:<>
         [division-form/form path]
         (let [options (division-options/options field)]
           [:<>
            (when (:line options)
              [line/form (conj path :line) :options (:line options)])
            (when (:opposite-line options)
              [line/form (conj path :opposite-line)
               :options (:opposite-line options)
               :defaults (options/sanitize (:line field) (:line options))
               :title "Opposite Line"])
            (when (:extra-line options)
              [line/form (conj path :extra-line)
               :options (:extra-line options)
               :defaults (options/sanitize (:line field) (:line options))
               :title "Extra Line"])
            (when (-> options :variant)
              [element/select (conj path :variant) "Variant"
               (-> options :variant :choices)
               :default (-> options :variant :default)])
            (when (-> options :thickness)
              [element/range-input-with-checkbox (conj path :thickness) "Thickness"
               (-> options :thickness :min)
               (-> options :thickness :max)
               :step 0.01
               :default (-> options :thickness :default)])
            (when (-> options :origin)
              [position/form (conj path :origin)
               :title "Origin"
               :options (:origin options)])
            (when (-> options :anchor)
              [position/form (conj path :anchor)
               :title "Anchor"
               :options (:anchor options)])
            (when (:geometry options)
              [geometry/form (conj path :geometry)
               (:geometry options)
               :current (:geometry field)])
            (when (:layout options)
              [form-for-layout path :options (:layout options)])])
         (if (= division-type :plain)
           [form-for-plain-field path]
           [element/checkbox (conj path :hints :outline?) "Outline"])])]
     (cond
       (#{:chequy
          :lozengy
          :vairy
          :potenty
          :papellony
          :masonry} division-type) [:div.parts.components {:style {:margin-bottom "0.5em"}}
                                    [:ul
                                     (let [tinctures @(rf/subscribe [:get (conj path :fields)])]
                                       (for [idx (range (count tinctures))]
                                         ^{:key idx}
                                         [:li
                                          [tincture/form (conj path :fields idx :tincture)
                                           :label (str "Tincture " (inc idx))]]))]]
       (not counterchanged?) [:div.parts.components
                              [:ul
                               (let [content @(rf/subscribe [:get (conj path :fields)])
                                     mandatory-part-count (division/mandatory-part-count field)]
                                 (for [[idx part] (map-indexed vector content)]
                                   (let [part-path (conj path :fields idx)
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
                                                                                       (-> (division/default-fields field)
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
