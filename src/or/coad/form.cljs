(ns or.coad.form
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.charge :as charge]
            [or.coad.config :as config]
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.line :as line]
            [or.coad.options :as options]
            [or.coad.ordinary :as ordinary]
            [or.coad.position :as position]
            [or.coad.tincture :as tincture]
            [or.coad.util :as util]
            [re-frame.core :as rf]))

;; components

(defn checkbox [path label & {:keys [on-change disabled? checked?]}]
  (let [component-id (util/id "checkbox")
        checked? (-> (and path
                          @(rf/subscribe [:get-in path]))
                     (or checked?)
                     boolean
                     (and (not disabled?)))]
    [:div.setting
     [:input {:type "checkbox"
              :id component-id
              :checked checked?
              :disabled disabled?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (if on-change
                              (on-change new-checked?)
                              (rf/dispatch [:set-in path new-checked?])))}]
     [:label {:for component-id} label]]))

(defn select [path label choices & {:keys [grouped? value on-change default]}]
  (let [component-id (util/id "select")
        current-value @(rf/subscribe [:get-in path])]
    [:div.setting
     [:label {:for component-id} (str label ":")]
     [:select {:id component-id
               :value (name (or value
                                current-value
                                default
                                :none))
               :on-change #(let [checked (keyword (-> % .-target .-value))]
                             (if on-change
                               (on-change checked)
                               (rf/dispatch [:set-in path checked])))}
      (if grouped?
        (for [[group-name & group-choices] choices]
          (if (and (-> group-choices count (= 1))
                   (-> group-choices first keyword?))
            (let [key (-> group-choices first)]
              ^{:key key}
              [:option {:value (name key)} group-name])
            ^{:key group-name}
            [:optgroup {:label group-name}
             (for [[display-name key] group-choices]
               ^{:key key}
               [:option {:value (name key)} display-name])]))
        (for [[display-name key] choices]
          ^{:key key}
          [:option {:value (name key)} display-name]))]]))

(defn range-input [path label min-value max-value & {:keys [value on-change default display-function step
                                                            disabled?]}]
  (let [component-id (util/id "range")
        checkbox-id (util/id "checkbox")
        current-value @(rf/subscribe [:get-in path])
        value (or value
                  current-value
                  default
                  min-value)
        using-default? (nil? current-value)
        checked? (not using-default?)]
    [:div.setting
     [:label {:for component-id} label]
     [:div.slider
      [:input {:type "checkbox"
               :id checkbox-id
               :checked checked?
               :disabled? disabled?
               :on-change #(let [new-checked? (-> % .-target .-checked)]
                             (if new-checked?
                               (rf/dispatch [:set-in path default])
                               (rf/dispatch [:remove-in path])))}]
      [:input {:type "range"
               :id component-id
               :min min-value
               :max max-value
               :step step
               :value value
               :disabled (or disabled?
                             using-default?)
               :on-change #(let [value (-> % .-target .-value js/parseFloat)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch [:set-in path value])))}]
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defn radio-select [path choices & {:keys [on-change default]}]
  [:div.setting
   (let [current-value (or @(rf/subscribe [:get-in path])
                           default)]
     (for [[display-name key] choices]
       (let [component-id (util/id "radio")]
         ^{:key key}
         [:<>
          [:input {:id component-id
                   :type "radio"
                   :value (name key)
                   :checked (= key current-value)
                   :on-change #(let [value (keyword (-> % .-target .-value))]
                                 (if on-change
                                   (on-change value)
                                   (rf/dispatch [:set-in path value])))}]
          [:label {:for component-id
                   :style {:margin-right "10px"}} display-name]])))])

(defn selector [path]
  [:a.selector {:on-click (fn [event]
                            (rf/dispatch [:select-component path])
                            (.stopPropagation event))}
   [:i.fas.fa-search]])

(defn component [path type title title-prefix & content]
  (let [selected? @(rf/subscribe [:get-in (conj path :ui :selected?)])
        flag-path (conj path :ui :open?)
        content? (seq content)
        open? (and @(rf/subscribe [:get-in flag-path])
                   content?)
        show-selector? (and (not= path [:render-options])
                            (get #{:field :ref} type))]
    [:div.component
     {:class (util/combine " " [(when type (name type))
                                (when selected? "selected")
                                (when (not open?) "closed")])}
     [:div.header.clickable {:on-click #(rf/dispatch [:toggle-in flag-path])}
      [:a.arrow {:style {:opacity (if content? 1 0)}}
       (if open?
         [:i.fas.fa-chevron-circle-down]
         [:i.fas.fa-chevron-circle-right])]
      [:h1 (util/combine " " [(when title-prefix
                                (str (util/upper-case-first title-prefix) ":"))
                              (when (and type
                                         (-> #{:field :ref}
                                             (get type)
                                             not))
                                (str (util/translate-cap-first type) ":"))
                              title])]
      (when show-selector?
        [selector path])]
     (when (and open?
                content?)
       (into [:div.content]
             content))]))

;; form

(declare form-for-field)

(defn submenu [path title link-name & content]
  (let [submenu-path [:ui :open-submenu path]
        submenu-open? @(rf/subscribe [:get-in submenu-path])]
    [:div.submenu-setting {:style {:display "inline-block"}}
     [:a {:on-click #(rf/dispatch [:set-in submenu-path true])}
      link-name]
     (when submenu-open?
       [:div.component.submenu
        #_{:on-mouse-leave #(rf/dispatch [:set-in submenu-path false])}
        [:div.header [:a {:on-click #(rf/dispatch [:set-in submenu-path false])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))

(defn replace-recursively [data value replacement]
  (walk/postwalk #(if (= % value)
                    replacement
                    %)
                 data))

(defn division-choice [path key display-name & {:keys [context]}]
  (let [render-shield (:render-shield context)
        value @(rf/subscribe [:get-division-type path])]
    [:div.choice.tooltip {:on-click #(rf/dispatch [:set-division-type path key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        [render-shield
         {:escutcheon :rectangle
          :field (if (= key :none)
                   {:component :field
                    :content {:tincture (if (= value key) :or :azure)}}
                   {:component :field
                    :division {:type key
                               :fields (-> (division/default-fields key)
                                           (replace-recursively :none :argent)
                                           (cond->
                                            (= value key) (replace-recursively :azure :or)))}})}
         {:outline? true}
         :db-path [:ui :division-option]]]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-division [path & {:keys [context]}]
  (let [division-type @(rf/subscribe [:get-division-type path])
        names (->> (into [["None" :none]]
                         division/choices)
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Division:"]
     " "
     [submenu path "Division" (get names division-type)
      (for [[display-name key] (into [["None" :none]]
                                     division/choices)]
        ^{:key key}
        [division-choice path key display-name :context context])]]))

(defn form-for-line [path & {:keys [title options] :or {title "Line"}}]
  (let [line @(rf/subscribe [:get-in path])
        type-names (->> line/choices
                        (map (comp vec reverse))
                        (into {}))]
    [:div.setting
     [:label (str title ":")]
     " "
     [submenu path "Line" (get type-names (:type line))
      [select (conj path :type) "Type" (-> options :type :choices)
       :default (options/get-value (:type line) (:type options))]
      (when (:eccentricity options)
        [range-input (conj path :eccentricity) "Eccentricity"
         (-> options :eccentricity :min)
         (-> options :eccentricity :max)
         :step 0.01
         :default (options/get-value (:eccentricity line) (:eccentricity options))])
      (when (:width options)
        [range-input (conj path :width) "Width"
         (-> options :width :min)
         (-> options :width :max)
         :default (options/get-value (:width line) (:width options))
         :display-function #(str % "%")])
      (when (:offset options)
        [range-input (conj path :offset) "Offset"
         (-> options :offset :min)
         (-> options :offset :max)
         :step 0.01
         :default (options/get-value (:offset line) (:offset options))])]]))

(defn form-for-position [path & {:keys [title options] :or {title "Position"}}]
  (let [position @(rf/subscribe [:get-in path])
        point-path (conj path :point)
        offset-x-path (conj path :offset-x)
        offset-y-path (conj path :offset-y)]
    [:div.setting
     [:label (str title ":")]
     " "
     [submenu path "Point" (str (-> position
                                    :point
                                    (or :fess)
                                    (util/translate-cap-first))
                                " point" (when (or (-> position :offset-x (or 0) zero? not)
                                                   (-> position :offset-y (or 0) zero? not))
                                           " (adjusted)"))
      [select point-path "Point" position/choices
       :on-change #(do
                     (rf/dispatch [:set-in point-path %])
                     (rf/dispatch [:set-in offset-x-path nil])
                     (rf/dispatch [:set-in offset-y-path nil]))]
      (when (:offset-x options)
        [range-input offset-x-path "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :default (options/get-value (:offset-x position) (:offset-x options))
         :display-function #(str % "%")])
      (when (:offset-y options)
        [range-input offset-y-path "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :default (options/get-value (:offset-y position) (:offset-y options))
         :display-function #(str % "%")])]]))

(defn tincture-choice [path key display-name & {:keys [context]}]
  (let [render-shield (:render-shield context)
        value @(rf/subscribe [:get-in path])]
    [:div.choice.tooltip {:on-click #(rf/dispatch [:set-in path key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        [render-shield
         {:escutcheon :rectangle
          :field {:component :field
                  :content {:tincture key}}}
         {:outline? true}
         :db-path [:ui :tincture-option]]]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-tincture [path & {:keys [context]}]
  (let [value @(rf/subscribe [:get-in path])
        names (->> (into [["None" :none]]
                         (->> tincture/choices
                              (map #(drop 1 %))
                              (apply concat)))
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Tincture:"]
     " "
     [submenu path "Tincture" (get names value)
      [tincture-choice path :none "None" :context context]
      (for [[group-name & group] tincture/choices]
        ^{:key group-name}
        [:<>
         [:h4 group-name]
         (for [[display-name key] group]
           ^{:key display-name}
           [tincture-choice path key display-name :context context])])]]))

(defn form-for-content [path & {:keys [context]}]
  [:div.form-content
   [form-for-tincture (conj path :tincture) :context context]])

(def node-icons
  {:group {:closed "fa-plus-square"
           :open "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open "fa-minus-square"}
   :charge {:closed "fa-plus-square"
            :open "fa-minus-square"}
   :variant {:normal "fa-image"}})

(defn form-for-ordinary [path & {:keys [parent-field context]}]
  (let [ordinary @(rf/subscribe [:get-in path])]
    [component
     path :ordinary (-> ordinary :type util/translate-cap-first) nil
     [:div.settings
      [select (conj path :type) "Type" ordinary/choices
       :on-change #(rf/dispatch [:set-ordinary-type path %])]
      (let [ordinary-options (ordinary/options ordinary)]
        [:<>
         (when (:line ordinary-options)
           [form-for-line (conj path :line) :options (:line ordinary-options) :context context])
         (when (:diagonal-mode ordinary-options)
           [select (conj path :diagonal-mode) "Diagonal"
            (-> ordinary-options :diagonal-mode :choices)
            :default (-> ordinary-options :diagonal-mode :default)])
         (when (:origin ordinary-options)
           [form-for-position (conj path :origin)
            :title "Origin"
            :options (:origin ordinary-options)])
         (when (:size ordinary-options)
           [range-input (conj path :size) "Size"
            (-> ordinary-options :size :min)
            (-> ordinary-options :size :max)
            :default (options/get-value (:size ordinary) (:size ordinary-options))
            :display-function #(str % "%")])])]
     [form-for-field (conj path :field) :parent-field parent-field :context context]]))

(defn tree-for-charge-map [{:keys [key type name groups charges attitudes variants]}
                           tree-path db-path
                           selected-charge remaining-path-to-charge & {:keys [charge-type
                                                                              charge-attitude
                                                                              still-on-path?]}]

  (let [flag-path (-> db-path
                      (concat [:ui :charge-map])
                      vec
                      (conj tree-path))
        db-open? @(rf/subscribe [:get-in flag-path])
        open? (or (= type :root)
                  (and (nil? db-open?)
                       still-on-path?)
                  db-open?)
        charge-type (if (= type :charge)
                      key
                      charge-type)
        charge-attitude (if (= type :attitude)
                          key
                          charge-attitude)]
    (cond-> [:<>]
      (not= type
            :root) (conj
                    [:span.node-name.clickable
                     {:on-click (if (= type :variant)
                                  #(rf/dispatch [:update-charge db-path {:type charge-type
                                                                         :attitude charge-attitude
                                                                         :variant key}])
                                  #(rf/dispatch [:toggle-in flag-path]))
                      :style {:color (when still-on-path? "#1b6690")}}
                     (if (= type :variant)
                       [:i.far {:class (-> node-icons (get type) :normal)}]
                       (if open?
                         [:i.far {:class (-> node-icons (get type) :open)}]
                         [:i.far {:class (-> node-icons (get type) :closed)}]))
                     [(cond
                        (and (= type :variant)
                             still-on-path?) :b
                        (= type :charge) :b
                        (= type :attitude) :em
                        :else :<>) name]])
      (and open?
           groups) (conj [:ul
                          (for [[key group] (sort-by first groups)]
                            (let [following-path? (and still-on-path?
                                                       (= (first remaining-path-to-charge)
                                                          key))
                                  remaining-path-to-charge (when following-path?
                                                             (drop 1 remaining-path-to-charge))]
                              ^{:key key}
                              [:li.group
                               [tree-for-charge-map
                                group
                                (conj tree-path :groups key)
                                db-path selected-charge
                                remaining-path-to-charge
                                :charge-type charge-type
                                :charge-attitude charge-attitude
                                :still-on-path? following-path?]]))])
      (and open?
           charges) (conj [:ul
                           (for [[key charge] (sort-by first charges)]
                             (let [following-path? (and still-on-path?
                                                        (-> remaining-path-to-charge
                                                            count zero?)
                                                        (= (:key charge)
                                                           (:type selected-charge)))]
                               ^{:key key}
                               [:li.charge
                                [tree-for-charge-map
                                 charge
                                 (conj tree-path :charges key)
                                 db-path selected-charge
                                 remaining-path-to-charge
                                 :charge-type charge-type
                                 :charge-attitude charge-attitude
                                 :still-on-path? following-path?]]))])
      (and open?
           attitudes) (conj [:ul
                             (for [[key attitude] (sort-by first attitudes)]
                               (let [following-path? (and still-on-path?
                                                          (-> remaining-path-to-charge
                                                              count zero?)
                                                          (= (:key attitude)
                                                             (:attitude selected-charge)))]
                                 ^{:key key}
                                 [:li.attitude
                                  [tree-for-charge-map
                                   attitude
                                   (conj tree-path :attitudes key)
                                   db-path selected-charge
                                   remaining-path-to-charge
                                   :charge-type charge-type
                                   :charge-attitude charge-attitude
                                   :still-on-path? following-path?]]))])
      (and open?
           variants) (conj [:ul
                            (for [[key variant] (sort-by first variants)]
                              (let [following-path? (and still-on-path?
                                                         (-> remaining-path-to-charge
                                                             count zero?)
                                                         (= (:key variant)
                                                            (:variant selected-charge)))]
                                ^{:key key}
                                [:li.variant
                                 [tree-for-charge-map
                                  variant
                                  (conj tree-path :variants key)
                                  db-path selected-charge
                                  remaining-path-to-charge
                                  :charge-type charge-type
                                  :charge-attitude charge-attitude
                                  :still-on-path? following-path?]]))]))))

(defn form-for-charge [path & {:keys [parent-field context]}]
  (let [charge @(rf/subscribe [:get-in path])
        charge-variant-data (charge/get-charge-variant-data charge)
        charge-map (charge/get-charge-map)
        supported-tinctures (-> charge-variant-data
                                :supported-tinctures
                                set)
        sorted-supported-tinctures (filter supported-tinctures [:armed :langued :attired :unguled])
        eyes-and-teeth-support (:eyes-and-teeth supported-tinctures)
        title (s/join " " [(-> charge :type util/translate-cap-first)
                           (-> charge :attitude util/translate)])]
    (if (and charge-map
             charge-variant-data)
      [component path :charge title nil
       [:div.setting
        [:label "Charge:"] " "
        [submenu path "Charge" title
         [:div.tree
          [tree-for-charge-map charge-map [] path charge
           (get-in charge-map
                   [:lookup (:type charge)])
           :still-on-path? true]]]]
       [:div.settings
        [:div.placeholders
         {:style {:width "50%"
                  :float "left"}}
         (for [t sorted-supported-tinctures]
           ^{:key t}
           [form-for-tincture
            (conj path :tincture t)
            (util/translate-cap-first t) :context context])]
        [:div
         {:style {:width "50%"
                  :float "left"}}
         (when eyes-and-teeth-support
           [checkbox
            (conj path :tincture :eyes-and-teeth)
            "White eyes and teeth"
            :on-change #(rf/dispatch [:set-in
                                      (conj path :tincture :eyes-and-teeth)
                                      (if % :argent nil)])])
         [checkbox (conj path :hints :outline?) "Draw outline"]]
        [:div.spacer]
        (let [charge-options (charge/options charge)]
          [:<>
           (when (:position charge-options)
             [form-for-position (conj path :position)
              :title "Position"
              :options (:position charge-options)])
           (when (:size charge-options)
             [range-input (conj path :size) "Size"
              (-> charge-options :size :min)
              (-> charge-options :size :max)
              :default (options/get-value (:size charge) (:size charge-options))
              :display-function #(str % "%")])])]
       [form-for-field (conj path :field) :parent-field parent-field :context context]]
      [:<>])))

(defn form-for-field [path & {:keys [parent-field title-prefix context]}]
  (let [division-type @(rf/subscribe [:get-division-type path])
        field @(rf/subscribe [:get-in path])
        counterchanged? (and @(rf/subscribe [:get-in (conj path :counterchanged?)])
                             (division/counterchangable? (-> parent-field :division :type)))
        root-field? (= path [:coat-of-arms :field])]
    [component path :field (cond
                             (:counterchanged? field) "Counterchanged"
                             (= division-type :none) (-> field :content :tincture util/translate-tincture util/upper-case-first)
                             :else (-> division-type util/translate-cap-first)) title-prefix
     [:div.settings
      (when (not root-field?)
        [checkbox (conj path :inherit-environment?) "Inherit environment (dimidiation)"])
      (when (and (not= path [:coat-of-arms :field])
                 parent-field)
        [checkbox (conj path :counterchanged?) "Counterchange"
         :disabled? (not (division/counterchangable? (-> parent-field :division :type)))])
      (when (not counterchanged?)
        [:<>
         [form-for-division path :context context]
         (let [division-options (division/options (:division field))]
           [:<>
            (when (:line division-options)
              [form-for-line (conj path :division :line) :options (:line division-options)])
            (when (:diagonal-mode division-options)
              [select (conj path :division :diagonal-mode) "Diagonal"
               (-> division-options :diagonal-mode :choices)
               :default (-> division-options :diagonal-mode :default)])
            (when (:origin division-options)
              [form-for-position (conj path :division :origin)
               :title "Origin"
               :options (:origin division-options)])])
         (when (= division-type :none)
           [form-for-content (conj path :content) :context context])])]
     (when (not counterchanged?)
       [:div.parts.components
        [:ul
         (let [content @(rf/subscribe [:get-in (conj path :division :fields)])
               mandatory-part-count (division/mandatory-part-count division-type)]
           (for [[idx part] (map-indexed vector content)]
             (let [part-path (conj path :division :fields idx)
                   part-name (division/part-name division-type idx)
                   ref (:ref part)]
               ^{:key idx}
               [:li
                [:div
                 (if ref
                   [component part-path :ref (str "Same as " (division/part-name division-type ref)) part-name]
                   [form-for-field part-path :title-prefix part-name :context context])]
                [:div {:style {:padding-left "10px"}}
                 (if ref
                   [:a {:on-click #(rf/dispatch [:set-in part-path
                                                 (-> (get content ref)
                                                     (assoc-in [:ui :open?] true))])}
                    [:i.far.fa-edit]]
                   (when (>= idx mandatory-part-count)
                     [:a {:on-click #(rf/dispatch [:set-in (conj part-path :ref)
                                                   (-> (division/default-fields division-type)
                                                       (get idx)
                                                       :ref)])}
                      [:i.far.fa-times-circle]]))]])))]])
     [:div {:style {:margin-bottom "0.5em"}}
      [:button {:on-click #(rf/dispatch [:add-component path (-> config/default-ordinary
                                                                 (assoc-in [:ui :open?] true)
                                                                 (assoc-in [:field :ui :open?] true))])}
       [:i.fas.fa-plus] " Add ordinary"]
      " "
      [:button {:on-click #(rf/dispatch [:add-component path (-> config/default-charge
                                                                 (assoc-in [:ui :open?] true)
                                                                 (assoc-in [:field :ui :open?] true))])}
       [:i.fas.fa-plus] " Add charge"]]
     [:div.components
      [:ul
       (let [components @(rf/subscribe [:get-in (conj path :components)])]
         (for [[idx component] (reverse (map-indexed vector components))]
           (let [component-path (conj path :components idx)]
             ^{:key idx}
             [:li
              [:div {:style {:padding-right "10px"
                             :white-space "nowrap"}}
               [:a (if (zero? idx)
                     {:class "disabled"}
                     {:on-click #(rf/dispatch [:move-component-down component-path])})
                [:i.fas.fa-chevron-down]]
               " "
               [:a (if (= idx (dec (count components)))
                     {:class "disabled"}
                     {:on-click #(rf/dispatch [:move-component-up component-path])})
                [:i.fas.fa-chevron-up]]]
              [:div
               (if (-> component :component (= :ordinary))
                 [form-for-ordinary component-path :parent-field field :context context]
                 [form-for-charge component-path :parent-field field :context context])]
              [:div {:style {:padding-left "10px"}}
               [:a {:on-click #(rf/dispatch [:remove-component component-path])}
                [:i.far.fa-trash-alt]]]])))]]]))

(defn escutcheon-choice [path key display-name & {:keys [context]}]
  (let [value @(rf/subscribe [:get-in path])
        render-shield (:render-shield context)]
    [:div.choice.tooltip {:on-click #(rf/dispatch [:set-in path key])}
     [:svg {:style {:width "4em"
                    :height "5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        [render-shield
         {:escutcheon key
          :field {:component :field
                  :content {:tincture (if (= value key) :or :azure)}}}
         {:outline? true}
         :db-path [:ui :escutcheon-option]]]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-escutcheon [path & {:keys [context]}]
  (let [escutcheon @(rf/subscribe [:get-in path])
        names (->> escutcheon/choices
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Escutcheon:"]
     " "
     [submenu path "Escutcheon" (get names escutcheon)
      (for [[display-name key] escutcheon/choices]
        ^{:key key}
        [escutcheon-choice path key display-name :context context])]
     [:div.spacer]]))

(defn form-render-options [& {:keys [context]}]
  [component [:render-options] :render-options "Options" nil
   [form-for-escutcheon [:coat-of-arms :escutcheon] :context context]
   (let [path [:render-options :mode]]
     [radio-select path [["Colours" :colours]
                         ["Hatching" :hatching]]
      :default :colours
      :on-change #(let [new-mode %]
                    (rf/dispatch [:set-in [:render-options :mode] new-mode])
                    (case new-mode
                      :hatching (rf/dispatch [:set :render-options :outline? true])
                      :colours (rf/dispatch [:set :render-options :outline? false])))])

   [checkbox [:render-options :outline?] "Draw outline"]
   [checkbox [:render-options :squiggly?] "Squiggly lines (experimental)"]
   [:div.setting
    [:button {:on-click #(rf/dispatch-sync [:set :coat-of-arms config/default-coat-of-arms])}
     "Clear shield"]]])
