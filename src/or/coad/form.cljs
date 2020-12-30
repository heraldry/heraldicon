(ns or.coad.form
  (:require [clojure.string :as s]
            [or.coad.charge :as charge]
            [or.coad.config :as config]
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.line :as line]
            [or.coad.ordinary :as ordinary]
            [or.coad.point :as point]
            [or.coad.tincture :as tincture]
            [or.coad.util :as util]
            [re-frame.core :as rf]))

;; helper

(def -current-id
  (atom 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))

;; components

(defn checkbox [path label & {:keys [on-change disabled? checked?]}]
  (let [component-id (id "checkbox")
        checked?     (-> (and path
                              @(rf/subscribe [:get-in path]))
                         (or checked?)
                         boolean
                         (and (not disabled?)))]
    [:div.setting
     [:input {:type      "checkbox"
              :id        component-id
              :checked   checked?
              :disabled  disabled?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (if on-change
                              (on-change new-checked?)
                              (rf/dispatch [:set-in path new-checked?])))}]
     [:label {:for component-id} label]]))

(defn select [path label options & {:keys [grouped? value on-change default]}]
  (let [component-id (id "select")]
    [:div.setting
     [:label {:for component-id} (str label ":")]
     [:select {:id        component-id
               :value     (name (or value
                                    @(rf/subscribe [:get-in path])
                                    default
                                    :none))
               :on-change #(let [checked (keyword (-> % .-target .-value))]
                             (if on-change
                               (on-change checked)
                               (rf/dispatch [:set-in path checked])))}
      (if grouped?
        (for [[group-name & group-options] options]
          (if (and (-> group-options count (= 1))
                   (-> group-options first keyword?))
            (let [key (-> group-options first)]
              ^{:key key}
              [:option {:value (name key)} group-name])
            ^{:key group-name}
            [:optgroup {:label group-name}
             (for [[display-name key] group-options]
               ^{:key key}
               [:option {:value (name key)} display-name])]))
        (for [[display-name key] options]
          ^{:key key}
          [:option {:value (name key)} display-name]))]]))

(defn range-input [path label min-value max-value & {:keys [value on-change default display-function step
                                                            disabled?]}]
  (let [component-id (id "range")
        value        (or value
                         @(rf/subscribe [:get-in path])
                         default
                         min-value)]
    [:div.setting
     [:label {:for component-id} label]
     [:div.slider
      [:input {:type      "range"
               :id        component-id
               :min       min-value
               :max       max-value
               :step      step
               :value     value
               :disabled  disabled?
               :on-change #(let [value (-> % .-target .-value js/parseFloat)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch [:set-in path value])))}]
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defn radio-select [path options & {:keys [on-change default]}]
  [:div.setting
   (let [current-value (or @(rf/subscribe [:get-in path])
                           default)]
     (for [[display-name key] options]
       (let [component-id (id "radio")]
         ^{:key key}
         [:<>
          [:input {:id        component-id
                   :type      "radio"
                   :value     (name key)
                   :checked   (= key current-value)
                   :on-change #(let [value (keyword (-> % .-target .-value))]
                                 (if on-change
                                   (on-change value)
                                   (rf/dispatch [:set-in path value])))}]
          [:label {:for   component-id
                   :style {:margin-right "10px"}} display-name]])))])

(defn selector [path]
  [:a.selector {:on-click (fn [event]
                            (rf/dispatch [:select-component path])
                            (.stopPropagation event))}
   [:i.fas.fa-search]])

(defn component [path type title title-prefix & content]
  (let [selected?      @(rf/subscribe [:get-in (conj path :ui :selected?)])
        flag-path      (conj path :ui :open?)
        content?       (seq content)
        open?          (and @(rf/subscribe [:get-in flag-path])
                            content?)
        show-selector? (and (not= path [:options])
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
  (let [submenu-path  [:ui :open-submenu path]
        submenu-open? @(rf/subscribe [:get-in submenu-path])]
    [:div.submenu-setting {:style {:display "inline-block"}}
     [:a {:on-click #(rf/dispatch [:set-in submenu-path true])}
      link-name]
     (when submenu-open?
       [:div.component.submenu
        {:on-mouse-leave #(rf/dispatch [:set-in submenu-path false])}
        [:div.header [:a {:on-click #(rf/dispatch [:set-in submenu-path false])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))

(defn form-for-line [path & {:keys [title] :or {title "Line"}}]
  (let [line        @(rf/subscribe [:get-in path])
        style-names (->> line/choices
                         (map (comp vec reverse))
                         (into {}))]
    [:div.setting
     [:label (str title ":")]
     " "
     [submenu path "Line" (get style-names (:style line))
      [select (conj path :style) "Type" line/choices]
      [range-input (conj path :eccentricity) "Eccentricity" 0.5 2 :step 0.01]
      [range-input (conj path :width) "Width" 2 100
       :display-function #(str % "%")]
      [range-input (conj path :offset) "Offset" -1 3 :step 0.01]]]))

(defn form-for-position [path & {:keys [title] :or {title "Position"}}]
  (let [point         @(rf/subscribe [:get-in path])
        point-path    (conj path :point)
        offset-x-path (conj path :offset-x)
        offset-y-path (conj path :offset-y)]
    [:div.setting
     [:label (str title ":")]
     " "
     [submenu path "Point" (str (-> point
                                    :point
                                    (or :fess)
                                    (util/translate-cap-first))
                                " point" (when (or (-> point :offset-x (or 0) zero? not)
                                                   (-> point :offset-y (or 0) zero? not))
                                           " (adjusted)"))
      [select point-path "Point" point/choices
       :on-change #(do
                     (rf/dispatch [:set-in point-path %])
                     (rf/dispatch [:set-in offset-x-path nil])
                     (rf/dispatch [:set-in offset-y-path nil]))]
      [range-input offset-x-path "Offset x" -50 50
       :step 1 :displat-function #(str % "%") :default 0]
      [range-input offset-y-path "Offset y" -50 50
       :step 1 :displat-function #(str % "%") :default 0]]]))

(defn form-for-tincture [path label]
  [:div.tincture
   [select path label (into [["None" :none]] tincture/choices) :grouped? true]])

(defn form-for-content [path]
  [:div.form-content
   [form-for-tincture (conj path :tincture) "Tincture"]])

(def node-icons
  {:group    {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :charge   {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :variant  {:normal "fa-image"}})

(defn form-for-ordinary [path & {:keys [parent-field]}]
  (let [ordinary      @(rf/subscribe [:get-in path])
        ordinary-type (:type ordinary)]
    [component
     path :ordinary (-> ordinary :type util/translate-cap-first) nil
     [:div.settings
      [select (conj path :type) "Type" ordinary/choices
       :on-change #(rf/dispatch [:set-ordinary-type path %])]
      (let [diagonal-options (ordinary/diagonal-options ordinary-type)]
        (when (-> diagonal-options count (> 0))
          [select (conj path :hints :diagonal-mode) "Diagonal"
           diagonal-options :default (ordinary/diagonal-default ordinary-type)]))
      (let [[min-value max-value] (ordinary/thickness-options ordinary-type)]
        (when min-value
          [range-input (conj path :hints :thickness) "Thickness" min-value max-value
           :display-function #(str % "%")
           :default (ordinary/thickness-default ordinary-type)]))
      [form-for-line (conj path :line)]
      (when (not (get #{:chief :base} ordinary-type))
        [form-for-position (conj path :origin)
         :title "Origin"])]
     [form-for-field (conj path :field) :parent-field parent-field]]))

(defn tree-for-charge-map [{:keys [key type name groups charges attitudes variants]}
                           tree-path db-path
                           selected-charge remaining-path-to-charge & {:keys [charge-type
                                                                              charge-attitude
                                                                              still-on-path?]}]

  (let [flag-path       (-> db-path
                            (concat [:ui :charge-map])
                            vec
                            (conj tree-path))
        db-open?        @(rf/subscribe [:get-in flag-path])
        open?           (or (= type :root)
                            (and (nil? db-open?)
                                 still-on-path?)
                            db-open?)
        charge-type     (if (= type :charge)
                          key
                          charge-type)
        charge-attitude (if (= type :attitude)
                          key
                          charge-attitude)]
    (cond-> [:<>]
      (not= type
            :root)    (conj
                       [:span.node-name.clickable
                        {:on-click (if (= type :variant)
                                     #(rf/dispatch [:update-charge db-path {:type     charge-type
                                                                            :attitude charge-attitude
                                                                            :variant  key}])
                                     #(rf/dispatch [:toggle-in flag-path]))
                         :style    {:color (when still-on-path? "#1b6690")}}
                        (if (= type :variant)
                          [:i.far {:class (-> node-icons (get type) :normal)}]
                          (if open?
                            [:i.far {:class (-> node-icons (get type) :open)}]
                            [:i.far {:class (-> node-icons (get type) :closed)}]))
                        [(cond
                           (and (= type :variant)
                                still-on-path?) :b
                           (= type :charge)     :b
                           (= type :attitude)   :em
                           :else                :<>) name]])
      (and open?
           groups)    (conj [:ul
                             (for [[key group] (sort-by first groups)]
                               (let [following-path?          (and still-on-path?
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
           charges)   (conj [:ul
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
           variants)  (conj [:ul
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

(defn form-for-charge [path & {:keys [parent-field]}]
  (let [charge                     @(rf/subscribe [:get-in path])
        charge-variant-data        (charge/get-charge-variant-data charge)
        charge-map                 (charge/get-charge-map)
        supported-tinctures        (-> charge-variant-data
                                       :supported-tinctures
                                       set)
        sorted-supported-tinctures (filter supported-tinctures [:armed :langued :attired :unguled])
        eyes-and-teeth-support     (:eyes-and-teeth supported-tinctures)
        title                      (s/join " " [(-> charge :type util/translate-cap-first)
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
            (util/translate-cap-first t)])]
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
        (let [size      (-> charge :hints :size)
              size-path (conj path :hints :size)]
          [:div.settings
           [:div
            {:style {:width "75%"
                     :float "left"}}
            [range-input size-path "Size" 1 100
             :display-function #(str % "%")
             :default 50
             :disabled? (-> size some? not)]]
           [:div
            {:style {:width "25%"
                     :float "right"}}
            [checkbox nil "auto"
             :checked? (-> size some? not)
             :on-change #(rf/dispatch [:set-in size-path (if % nil 50)])]]])
        [:div.spacer]
        [form-for-position (conj path :position)]]
       [form-for-field (conj path :field) :parent-field parent-field]]
      [:<>])))

(defn form-for-field [path & {:keys [parent-field title-prefix]}]
  (let [division-type   @(rf/subscribe [:get-division-type path])
        field           @(rf/subscribe [:get-in path])
        counterchanged? (and @(rf/subscribe [:get-in (conj path :counterchanged?)])
                             (division/counterchangable? (-> parent-field :division :type)))
        root-field?     (= path [:coat-of-arms :field])]
    [component path :field (cond
                             (:counterchanged? field) "Counterchanged"
                             (= division-type :none)  (-> field :content :tincture util/translate-tincture util/upper-case-first)
                             :else                    (-> division-type util/translate-cap-first)) title-prefix
     [:div.settings
      (when (not root-field?)
        [checkbox (conj path :inherit-environment?) "Inherit environment (dimidiation)"])
      (when (and (not= path [:coat-of-arms :field])
                 parent-field)
        [checkbox (conj path :counterchanged?) "Counterchange"
         :disabled? (not (division/counterchangable? (-> parent-field :division :type)))])
      (when (not counterchanged?)
        [:<>
         [:div.setting
          [:label "Division:"] " "
          [submenu (conj path :division) "Division" (-> division-type util/translate-cap-first)
           [select path "Type"
            (into [["None" :none]] division/choices)
            :value division-type
            :on-change #(rf/dispatch [:set-division-type path %])]]]
         (when (not= division-type :none)
           [form-for-line (conj path :division :line)])
         (when (not= division-type :none)
           (let [diagonal-options (division/diagonal-options division-type)]
             (when (-> diagonal-options count (> 0))
               [select (conj path :division :hints :diagonal-mode) "Diagonal"
                diagonal-options :default (division/diagonal-default division-type)])))
         (when (not= division-type :none)
           [form-for-position (conj path :division :origin)
            :title "Origin"])
         (when (= division-type :none)
           [form-for-content (conj path :content)])])]
     (when (not counterchanged?)
       [:div.parts.components
        [:ul
         (let [content              @(rf/subscribe [:get-in (conj path :division :fields)])
               mandatory-part-count (division/mandatory-part-count division-type)]
           (for [[idx part] (map-indexed vector content)]
             (let [part-path (conj path :division :fields idx)
                   part-name (division/part-name division-type idx)
                   ref       (:ref part)]
               ^{:key idx}
               [:li
                [:div
                 (if ref
                   [component part-path :ref (str "Same as " (division/part-name division-type ref)) part-name]
                   [form-for-field part-path :title-prefix part-name])]
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
                             :white-space   "nowrap"}}
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
                 [form-for-ordinary component-path :parent-field field]
                 [form-for-charge component-path :parent-field field])]
              [:div {:style {:padding-left "10px"}}
               [:a {:on-click #(rf/dispatch [:remove-component component-path])}
                [:i.far.fa-trash-alt]]]])))]]]))

(defn form-options []
  [component [:options] :options "Options" nil
   [select [:coat-of-arms :escutcheon] "Escutcheon" escutcheon/choices]
   (let [path [:options :mode]]
     [radio-select path [["Colours" :colours]
                         ["Hatching" :hatching]]
      :default :colours
      :on-change #(let [new-mode %]
                    (rf/dispatch [:set-in [:options :mode] new-mode])
                    (case new-mode
                      :hatching (rf/dispatch [:set :options :outline? true])
                      :colours  (rf/dispatch [:set :options :outline? false])))])

   [checkbox [:options :outline?] "Draw outline"]
   [checkbox [:options :squiggly?] "Squiggly lines (experimental)"]
   [:div.setting
    [:button {:on-click #(rf/dispatch-sync [:set :coat-of-arms config/default-coat-of-arms])}
     "Clear shield"]]])
