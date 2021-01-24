(ns heraldry.frontend.form.component
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.charge :as charge]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary :as ordinary]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.frontend.charge-map :as charge-map]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [id]]
            [re-frame.core :as rf]))

(def coa-select-option-context
  (-> context/default
      (dissoc :fn-component-selected?)
      (dissoc :fn-select-component)))

;; subs

(rf/reg-sub
 :ui-component-open?
 (fn [db [_ path]]
   (get-in db [:ui :component-open? path])))

(rf/reg-sub
 :ui-submenu-open?
 (fn [db [_ path]]
   (get-in db [:ui :submenu-open? path])))

(rf/reg-sub
 :ui-component-selected?
 (fn [db [_ path]]
   (or (get-in db [:ui :component-selected? path])
       (when (get-in db (-> path
                            (->> (drop-last 3))
                            vec
                            (conj :counterchanged?)))
         (let [parent-field-path (-> path
                                     (->> (drop-last 6))
                                     vec
                                     (conj :division :fields (last path)))]
           (get-in db [:ui :component-selected? parent-field-path]))))))


;; events


(rf/reg-event-db
 :ui-component-open
 (fn [db [_ path]]
   (-> (loop [db db
              rest path]
         (if (empty? rest)
           db
           (recur
            (if (get-in db (conj rest :component))
              (assoc-in db [:ui :component-open? rest] true)
              db)
            (-> rest drop-last vec)))))))

(rf/reg-event-db
 :ui-component-close
 (fn [db [_ path]]

   (update-in db [:ui :component-open?]
              #(into {}
                     (->> %
                          (filter (fn [[k _]]
                                    (not (and (-> k count (>= (count path)))
                                              (= (subvec k 0 (count path))
                                                 path))))))))))

(rf/reg-event-fx
 :ui-component-open-toggle
 (fn [{:keys [db]} [_ path]]
   (let [open? (get-in db [:ui :component-open? path])]
     (if open?
       {:fx [[:dispatch [:ui-component-close path]]]}
       {:fx [[:dispatch [:ui-component-open path]]]}))))

(rf/reg-event-db
 :ui-component-deselect-all
 (fn [db _]
   (update-in db [:ui] dissoc :component-selected?)))

(rf/reg-event-db
 :ui-submenu-close-all
 (fn [db _]
   (update-in db [:ui] dissoc :submenu-open?)))

(rf/reg-event-db
 :ui-submenu-open
 (fn [db [_ path]]
   (assoc-in db [:ui :submenu-open? path] true)))

(rf/reg-event-db
 :ui-submenu-close
 (fn [db [_ path]]
   (assoc-in db [:ui :submenu-open? path] false)))

(rf/reg-event-fx
 :ui-component-select
 (fn [{:keys [db]} [_ path]]
   (let [real-path (if (get-in
                        db
                        (-> path
                            (->> (drop-last 3))
                            vec
                            (conj :counterchanged?)))
                     (-> path
                         (->> (drop-last 6))
                         vec
                         (conj :division :fields (last path)))
                     path)]
     {:db (-> db
              (update-in [:ui] dissoc :component-selected?)
              (cond->
               path (as-> db
                          (assoc-in db [:ui :component-selected? real-path] true))))
      :fx [[:dispatch [:ui-component-open real-path]]]})))

(rf/reg-event-db
 :set-division-type
 (fn [db [_ path new-type]]
   (if (= new-type :none)
     (-> db
         (update-in path dissoc :division)
         (update-in (conj path :content) #(or % {:tincture :none})))
     (-> db
         (assoc-in (conj path :division :type) new-type)
         (update-in (conj path :division :line :type) #(or % :straight))
         (update-in (conj path :division :fields)
                    (fn [current-value]
                      (let [current (or current-value [])
                            current-type (get-in db (conj path :division :type))
                            current-mandatory-part-count (division/mandatory-part-count current-type)
                            new-mandatory-part-count (division/mandatory-part-count new-type)
                            min-mandatory-part-count (min current-mandatory-part-count
                                                          new-mandatory-part-count)
                            current (if (or (= current-mandatory-part-count
                                               new-mandatory-part-count)
                                            (<= (count current) min-mandatory-part-count))
                                      current
                                      (subvec current 0 min-mandatory-part-count))
                            default (division/default-fields new-type)]
                        (cond
                          (< (count current) (count default)) (into current (subvec default (count current)))
                          (> (count current) (count default)) (subvec current 0 (count default))
                          :else current))))
         (update-in (conj path :division) #(merge %
                                                  (options/sanitize-or-nil % (division/options %))))
         (update-in path dissoc :content)
         (cond->
          (not (division/counterchangable? {:type new-type})) (update-in (conj path :components)
                                                                         (fn [components]
                                                                           (->> components
                                                                                (map #(update % :field dissoc :counterchanged?))
                                                                                vec))))))))

(rf/reg-event-db
 :set-ordinary-type
 (fn [db [_ path new-type]]
   (-> db
       (assoc-in (conj path :type) new-type)
       (update-in path #(merge %
                               (options/sanitize-or-nil % (ordinary/options %)))))))
(rf/reg-event-db
 :set-charge-type
 (fn [db [_ path new-type]]
   (-> db
       (assoc-in (conj path :type) new-type)
       (update-in path #(merge %
                               (options/sanitize-or-nil % (charge/options %)))))))

(rf/reg-event-fx
 :add-component
 (fn [{:keys [db]} [_ path value]]
   (let [components-path (conj path :components)
         index (count (get-in db components-path))]
     {:db (update-in db components-path #(-> %
                                             (conj value)
                                             vec))
      :fx [[:dispatch [:ui-submenu-open (conj components-path index (case (:component value)
                                                                      :ordinary "Select Ordinary"
                                                                      :charge "Select Charge"))]]
           [:dispatch [:ui-component-open (conj components-path index)]]
           [:dispatch [:ui-component-open (conj components-path index :field)]]]})))

(rf/reg-event-db
 :remove-component
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index (last path)]
     (update-in db components-path (fn [components]
                                     (vec (concat (subvec components 0 index)
                                                  (subvec components (inc index)))))))))

(rf/reg-event-db
 :move-component-up
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index (last path)]
     (update-in db components-path (fn [components]
                                     (let [num-components (count components)]
                                       (if (>= index num-components)
                                         components
                                         (-> components
                                             (subvec 0 index)
                                             (conj (get components (inc index)))
                                             (conj (get components index))
                                             (concat (subvec components (+ index 2)))
                                             vec))))))))

(rf/reg-event-db
 :move-component-down
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index (last path)]
     (update-in db components-path (fn [components]
                                     (if (zero? index)
                                       components
                                       (-> components
                                           (subvec 0 (dec index))
                                           (conj (get components index))
                                           (conj (get components (dec index)))
                                           (concat (subvec components (inc index)))
                                           vec)))))))

(rf/reg-event-db
 :update-charge
 (fn [db [_ path changes]]
   (update-in db path merge changes)))

;; components

(declare form-for-field)

(defn checkbox [path label & {:keys [on-change disabled? checked?]}]
  (let [component-id (id "checkbox")
        checked? (-> (and path
                          @(rf/subscribe [:get path]))
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
                              (rf/dispatch [:set path new-checked?])))}]
     [:label {:for component-id} label]]))

(defn select [path label choices & {:keys [grouped? value on-change default]}]
  (let [component-id (id "select")
        current-value @(rf/subscribe [:get path])]
    [:div.setting
     [:label {:for component-id} label]
     [:select {:id component-id
               :value (name (or value
                                current-value
                                default
                                :none))
               :on-change #(let [checked (keyword (-> % .-target .-value))]
                             (if on-change
                               (on-change checked)
                               (rf/dispatch [:set path checked])))}
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

(defn radio-select [path choices & {:keys [on-change default]}]
  [:div.setting
   (let [current-value (or @(rf/subscribe [:get path])
                           default)]
     (for [[display-name key] choices]
       (let [component-id (id "radio")]
         ^{:key key}
         [:<>
          [:input {:id component-id
                   :type "radio"
                   :value (name key)
                   :checked (= key current-value)
                   :on-change #(let [value (keyword (-> % .-target .-value))]
                                 (if on-change
                                   (on-change value)
                                   (rf/dispatch [:set path value])))}]
          [:label {:for component-id
                   :style {:margin-right "10px"}} display-name]])))])

(defn range-input [path label min-value max-value & {:keys [value on-change default display-function step
                                                            disabled?]}]
  (let [component-id (id "range")
        checkbox-id (id "checkbox")
        current-value @(rf/subscribe [:get path])
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
                               (rf/dispatch [:set path default])
                               (rf/dispatch [:remove path])))}]
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
                               (rf/dispatch [:set path value])))}]
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defn selector [path]
  [:a.selector {:on-click #(state/dispatch-on-event % [:ui-component-select path])}
   [:i.fas.fa-search]])

(defn component [path type title title-prefix & content]
  (let [selected? @(rf/subscribe [:ui-component-selected? path])
        content? (seq content)
        open? (and @(rf/subscribe [:ui-component-open? path])
                   content?)
        show-selector? (and (not= path [:render-options])
                            (get #{:field :ref} type))]
    [:div.component
     {:class (util/combine " " [(when type (name type))
                                (when selected? "selected")
                                (when (not open?) "closed")])}
     [:div.header.clickable {:on-click #(state/dispatch-on-event % [:ui-component-open-toggle path])}
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

(defn submenu [path title link-name styles & content]
  (let [submenu-id (conj path title)
        submenu-open? @(rf/subscribe [:ui-submenu-open? submenu-id])]
    [:div.submenu-setting {:style {:display "inline-block"}
                           :on-click #(.stopPropagation %)}
     [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-open submenu-id])}
      link-name]
     (when submenu-open?
       [:div.component.submenu {:style styles}
        [:div.header [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-close submenu-id])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))

(defn escutcheon-choice [path key display-name]
  (let [value @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon key
                           :field {:component :field
                                   :content {:tincture (if (= value key) :or :azure)}}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style {:width "4em"
                    :height "5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-escutcheon [path]
  (let [escutcheon (or @(rf/subscribe [:get path])
                       :heater)
        names (->> escutcheon/choices
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Escutcheon"]
     " "
     [submenu path "Select Escutcheon" (get names escutcheon) {:min-width "17.5em"}
      (for [[display-name key] escutcheon/choices]
        ^{:key key}
        [escutcheon-choice path key display-name])]
     [:div.spacer]]))

(defn form-render-options [db-path]
  [component (conj db-path :render-options) :render-options "Options" nil
   [form-for-escutcheon (conj db-path :coat-of-arms :escutcheon)]
   (let [mode-path (conj db-path :render-options :mode)
         outline-path (conj db-path :render-options :outline?)]
     [radio-select mode-path [["Colours" :colours]
                              ["Hatching" :hatching]]
      :default :colours
      :on-change #(let [new-mode %]
                    (rf/dispatch [:set mode-path new-mode])
                    (case new-mode
                      :hatching (rf/dispatch [:set outline-path true])
                      :colours (rf/dispatch [:set outline-path false])))])
   [checkbox (conj db-path :render-options :outline?) "Draw outline"]
   [checkbox (conj db-path :render-options :squiggly?) "Squiggly lines (can be slow)"]])

(defn tincture-choice [path key display-name]
  (let [value @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:component :field
                                   :content {:tincture key}}}
                          40
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                          :style {:border (if (= value key)
                                            "1px solid #000"
                                            "1px solid transparent")
                                  :border-radius "5px"}}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 50 100"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(5,5)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-tincture [path & {:keys [label] :or {label "Tincture"}}]
  (let [value (or @(rf/subscribe [:get path])
                  :none)
        names (->> (into [["None" :none]]
                         (->> tincture/choices
                              (map #(drop 1 %))
                              (apply concat)))
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label label]
     " "
     [submenu path "Select Tincture" (get names value) {:min-width "22em"}
      (for [[group-name & group] tincture/choices]
        ^{:key group-name}
        [:<>
         (if (= group-name "Metal")
           [:<>
            [:h4 {:style {:margin-left "4.5em"}} group-name]
            [tincture-choice path :none "None"]]
           [:h4 group-name])
         (for [[display-name key] group]
           ^{:key display-name}
           [tincture-choice path key display-name])])]]))

(defn form-for-position [path & {:keys [title options] :or {title "Position"}}]
  (let [position @(rf/subscribe [:get path])
        point-path (conj path :point)
        offset-x-path (conj path :offset-x)
        offset-y-path (conj path :offset-y)]
    [:div.setting
     [:label title]
     " "
     [submenu path title (str (-> position
                                  :point
                                  (or :fess)
                                  (util/translate-cap-first))
                              " point" (when (or (-> position :offset-x (or 0) zero? not)
                                                 (-> position :offset-y (or 0) zero? not))
                                         " (adjusted)")) {}
      [select point-path "Point" (-> options :point :choices)
       :on-change #(do
                     (rf/dispatch [:set point-path %])
                     (rf/dispatch [:set offset-x-path nil])
                     (rf/dispatch [:set offset-y-path nil]))]
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

(defn form-for-geometry [path options & {:keys [current]}]
  (let [changes (filter some? [(when (and (:size current)
                                          (:size options)) "resized")
                               (when (and (:stretch current)
                                          (:stretch options)) "stretched")
                               (when (and (:rotation current)
                                          (:rotation options)) "rotated")
                               (when (and (:mirrored? current)
                                          (:mirrored? options)) "mirrored")
                               (when (and (:reversed? current)
                                          (:reversed? options)) "reversed")])
        current-display (-> (if (-> changes count (> 0))
                              (util/combine ", " changes)
                              "default")
                            util/upper-case-first)]
    [:div.setting
     [:label "Geometry"]
     " "
     [submenu path "Geometry" current-display {}
      [:div.settings
       (when (:size options)
         [range-input (conj path :size) "Size"
          (-> options :size :min)
          (-> options :size :max)
          :default (options/get-value (:size current) (:size options))
          :display-function #(str % "%")])
       (when (:stretch options)
         [range-input (conj path :stretch) "Stretch"
          (-> options :stretch :min)
          (-> options :stretch :max)
          :step 0.01
          :default (options/get-value (:stretch current) (:stretch options))])
       (when (:rotation options)
         [range-input (conj path :rotation) "Rotation"
          (-> options :rotation :min)
          (-> options :rotation :max)
          :step 5
          :default (options/get-value (:rotation current) (:rotation options))])
       (when (:mirrored? options)
         [checkbox (conj path :mirrored?) "Mirrored"])
       (when (:reversed? options)
         [checkbox (conj path :reversed?) "Reversed"])]]]))

(defn charge-type-choice [path key display-name & {:keys [current]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:component :field
                                   :content {:tincture :argent}
                                   :components [{:component :charge
                                                 :type key
                                                 :geometry {:size 75}
                                                 :escutcheon (if (= key :escutcheon) :heater nil)
                                                 :field {:content {:tincture (if (= current key) :or :azure)}}}]}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:update-charge path {:type key
                                                                                      :attitude nil
                                                                                      :facing nil
                                                                                      :data nil}])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn charge-type-selected-choice [charge display-name]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:component :field
                                   :content {:tincture :argent}
                                   :components [{:component :charge
                                                 :type (:type charge)
                                                 :variant (:variant charge)
                                                 :field {:content {:tincture :or}}}]}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(def node-icons
  {:group {:closed "fa-plus-square"
           :open "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open "fa-minus-square"}
   :facing {:closed "fa-plus-square"
            :open "fa-minus-square"}
   :charge {:closed "fa-plus-square"
            :open "fa-minus-square"}
   :variant {:normal "fa-image"}})

(defn count-variants [node]
  (cond
    (-> node nil?) 0
    (-> node :type (= :variant)) 1
    :else (->> [:groups :charges :attitudes :facings :variants]
               (map (fn [key]
                      (-> node
                          (get key)
                          (->> (map (fn [[_ v]]
                                      (count-variants v)))
                               (reduce +)))))
               (reduce +))))

(defn tree-for-charge-map [{:keys [type name groups charges attitudes facings variants] :as node}
                           tree-path db-path
                           selected-charge remaining-path-to-charge & {:keys [still-on-path?]}]
  (let [flag-path (conj [:ui :charge-map] tree-path)
        db-open? @(rf/subscribe [:get flag-path])
        open? (or (= type :_root)
                  (and (nil? db-open?)
                       still-on-path?)
                  db-open?)]
    (cond-> [:<>]
      (not= type
            :_root) (conj
                     [:span.node-name.clickable
                      {:on-click (if (= type :variant)
                                   #(state/dispatch-on-event
                                     %
                                     [:update-charge
                                      db-path
                                      (let [charge-data (:data node)]
                                        (merge {:type (:type charge-data)
                                                :variant {:id (:id charge-data)
                                                          :version (:version charge-data)}}
                                               (select-keys charge-data
                                                            [:attitude :facing])))])
                                   #(state/dispatch-on-event % [:toggle flag-path]))
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
                         (= type :facing) :em
                         :else :<>) name (when (not= type :variant)
                                           (str " (" (count-variants node) ")"))]])
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
                                   :still-on-path? following-path?]]))])
      (and open?
           facings) (conj [:ul
                           (for [[key facing] (sort-by first facings)]
                             (let [following-path? (and still-on-path?
                                                        (-> remaining-path-to-charge
                                                            count zero?)
                                                        (= (:key facing)
                                                           (:facing selected-charge)))]
                               ^{:key key}
                               [:li.variant
                                [tree-for-charge-map
                                 facing
                                 (conj tree-path :facings key)
                                 db-path selected-charge
                                 remaining-path-to-charge
                                 :still-on-path? following-path?]]))])
      (and open?
           variants) (conj [:ul
                            (for [[key variant] (sort-by (comp :name second) variants)]
                              (let [following-path? (and still-on-path?
                                                         (-> remaining-path-to-charge
                                                             count zero?)
                                                         (= (:key variant)
                                                            (:facing selected-charge)))]
                                ^{:key key}
                                [:li.variant
                                 [tree-for-charge-map
                                  variant
                                  (conj tree-path :variants key)
                                  db-path selected-charge
                                  remaining-path-to-charge
                                  :still-on-path? following-path?]]))]))))

(defn charge-type-more-choice [path charge]
  [submenu path "Select Other Charge"
   (let [{:keys [result]} (render/coat-of-arms
                           {:escutcheon :rectangle
                            :field {:component :field
                                    :content {:tincture :argent}
                                    :components [{:component :charge
                                                  :type :roundel
                                                  :geometry {:size 10}
                                                  :field {:content {:tincture :azure}}}
                                                 {:component :charge
                                                  :type :roundel
                                                  :geometry {:size 10}
                                                  :position {:offset-x -15}
                                                  :field {:content {:tincture :azure}}}
                                                 {:component :charge
                                                  :type :roundel
                                                  :geometry {:size 10}
                                                  :position {:offset-x 15}
                                                  :field {:content {:tincture :azure}}}]}}
                           100
                           (-> coa-select-option-context
                               (assoc-in [:render-options :outline?] true)))]
     [:div.choice.tooltip
      [:svg {:style {:width "4em"
                     :height "4.5em"}
             :viewBox "0 0 120 200"
             :preserveAspectRatio "xMidYMin slice"}
       [:g {:filter "url(#shadow)"}
        [:g {:transform "translate(10,10)"}
         result]]]
      [:div.bottom
       [:h3 {:style {:text-align "center"}} "more"]
       [:i]]])
   {}
   [:div.tree
    (let [charge-map (charge-map/get-charge-map)]
      (if charge-map
        [tree-for-charge-map charge-map [] path charge
         (get-in charge-map
                 [:lookup (:type charge)])
         :still-on-path? true]
        [:div "loading..."]))]])

(defn form-for-charge-type [path]
  (let [charge @(rf/subscribe [:get path])
        charge-type (:type charge)
        names (->> charge/choices
                   (map (comp vec reverse))
                   (into {}))
        title (util/combine " " [(or (get names charge-type)
                                     (-> charge :type util/translate-cap-first))
                                 (-> charge :attitude util/translate)
                                 (-> charge :facing util/translate)])]
    [:div.setting
     [:label "Type"]
     " "
     [submenu path "Select Charge" title {:min-width "17.5em"}
      (for [[display-name key] charge/choices]
        ^{:key key}
        [charge-type-choice path key display-name :current charge-type])
      (when (-> names (contains? charge-type) not)
        [charge-type-selected-choice charge title])
      [charge-type-more-choice path charge]]]))

(defn form-for-charge [path & {:keys [parent-field]}]
  (let [charge @(rf/subscribe [:get path])
        supported-tinctures #{:armed :langued :attired :unguled
                              :beaked}
        sorted-supported-tinctures (filter supported-tinctures
                                           [:armed :langued :attired :unguled
                                            :beaked])
        eyes-and-teeth-support true
        tinctures-set (-> charge
                          :tincture
                          (->> (filter (fn [[_ v]]
                                         (and (some? v)
                                              (not= v :none))))
                               (map first)
                               set)
                          (filter
                           [:armed :langued :attired :unguled
                            :beaked :eyes-and-teeth])
                          (->> (map util/translate-cap-first)))
        tinctures-title (if (-> tinctures-set count pos?)
                          (util/combine ", " tinctures-set)
                          "Set")
        tinctures-title (if (-> tinctures-title count (> 30))
                          (str (subs tinctures-title 0 27) "...")
                          tinctures-title)
        title (s/join " " [(-> charge :type util/translate-cap-first)
                           (-> charge :attitude util/translate)])]
    [component path :charge title nil
     [:div.settings
      (when (and (:type charge)
                 (-> charge :type :map? not))
        [form-for-charge-type path])
      [:div.setting
       [:label "Tinctures"]
       " "
       [submenu (conj path :tincture) "Tinctures" tinctures-title {}
        (when sorted-supported-tinctures
          [:div.placeholders
           {:style {:width "50%"
                    :float "left"}}
           (for [t sorted-supported-tinctures]
             ^{:key t}
             [form-for-tincture
              (conj path :tincture t)
              :label (util/translate-cap-first t)])])
        [:div
         {:style {:width "50%"
                  :float "left"}}
         (when eyes-and-teeth-support
           [checkbox
            (conj path :tincture :eyes-and-teeth)
            "White eyes and teeth"
            :on-change #(rf/dispatch [:set
                                      (conj path :tincture :eyes-and-teeth)
                                      (if % :argent nil)])])]
        [:div.spacer]]]
      (let [charge-options (charge/options charge)]
        [:<>
         (when (:position charge-options)
           [form-for-position (conj path :position)
            :title "Position"
            :options (:position charge-options)])
         (when (:geometry charge-options)
           [form-for-geometry (conj path :geometry)
            (:geometry charge-options)
            :current (:geometry charge)])])
      [select (conj path :hints :outline-mode) "Outline" [["Keep" :keep]
                                                          ["Remove" :remove]
                                                          ["Primary" :primary]
                                                          ["Transparent" :transparent]]
       :default :keep]]
     [form-for-field (conj path :field) :parent-field parent-field]]))

(defn division-choice [path key display-name]
  (let [value (-> @(rf/subscribe [:get path])
                  :division
                  :type
                  (or :none))
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field (if (= key :none)
                                    {:component :field
                                     :content {:tincture (if (= value key) :or :azure)}}
                                    {:component :field
                                     :division {:type key
                                                :fields (-> (division/default-fields key)
                                                            (util/replace-recursively :none :argent)
                                                            (cond->
                                                             (= value key) (util/replace-recursively :azure :or)))}})}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-division-type path key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-division [path]
  (let [division-type (-> @(rf/subscribe [:get path])
                          :division
                          :type
                          (or :none))
        names (->> (into [["None" :none]]
                         division/choices)
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Division"]
     " "
     [submenu path "Select Division" (get names division-type) {:min-width "17.5em"}
      (for [[display-name key] (into [["None" :none]]
                                     division/choices)]
        ^{:key key}
        [division-choice path key display-name])]]))

(defn line-type-choice [path key display-name & {:keys [current]}]
  (let [options (line/options {:type key})
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :flag
                           :field {:component :field
                                   :division {:type :per-fess
                                              :line {:type key
                                                     :width (* 2 (options/get-value nil (:width options)))}
                                              :fields [{:content {:tincture :argent}}
                                                       {:content {:tincture (if (= key current) :or :azure)}}]}}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style {:width "6.5em"
                    :height "4.5em"}
            :viewBox "0 0 120 80"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-line-type [path & {:keys [options can-disable? default value]}]
  (let [line @(rf/subscribe [:get path])
        value (or value
                  (options/get-value (:type line) (:type options)))
        type-names (->> line/choices
                        (map (comp vec reverse))
                        (into {}))]
    [:div.setting
     [:label "Type"]
     [:div.other {:style {:display "inline-block"}}
      (when can-disable?
        [:input {:type "checkbox"
                 :checked (some? (:type line))
                 :on-change #(let [new-checked? (-> % .-target .-checked)]
                               (if new-checked?
                                 (rf/dispatch [:set (conj path :type) default])
                                 (rf/dispatch [:remove (conj path :type)])))}])
      (if (some? (:type line))
        [submenu (conj path :type) "Select Line Type" (get type-names value) {:min-width "21em"}
         (for [[display-name key] (-> options :type :choices)]
           ^{:key display-name}
           [line-type-choice (conj path :type) key display-name :current value])]
        (when can-disable?
          [:span {:style {:color "#ccc"}} (get type-names value)
           " (inherited)"]))]]))

(defn form-for-line [path & {:keys [title options defaults] :or {title "Line"}}]
  (let [line @(rf/subscribe [:get path])
        type-names (->> line/choices
                        (map (comp vec reverse))
                        (into {}))
        line-type (or (:type line)
                      (:type defaults))
        line-eccentricity (or (:eccentricity line)
                              (:eccentricity defaults))
        line-width (or (:width line)
                       (:width defaults))
        line-offset (or (:offset line)
                        (:offset defaults))]
    [:div.setting
     [:label title]
     " "
     [submenu path title (get type-names line-type) {}
      [form-for-line-type path :options options
       :can-disable? (some? defaults)
       :value line-type
       :default (:type defaults)]
      (when (:eccentricity options)
        [range-input (conj path :eccentricity) "Eccentricity"
         (-> options :eccentricity :min)
         (-> options :eccentricity :max)
         :step 0.01
         :default (or (:eccentricity defaults)
                      (options/get-value line-eccentricity (:eccentricity options)))])
      (when (:width options)
        [range-input (conj path :width) "Width"
         (-> options :width :min)
         (-> options :width :max)
         :default (or (:width defaults)
                      (options/get-value line-width (:width options)))
         :display-function #(str % "%")])
      (when (:offset options)
        [range-input (conj path :offset) "Offset"
         (-> options :offset :min)
         (-> options :offset :max)
         :step 0.01
         :default (or (:offset defaults)
                      (options/get-value line-offset (:offset options)))])
      (when (:flipped? options)
        [checkbox (conj path :flipped?) "Flipped"])]]))

(defn form-for-content [path]
  [:div.form-content
   [form-for-tincture (conj path :tincture)]])

(defn ordinary-type-choice [path key display-name & {:keys [current]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:component :field
                                   :content {:tincture :argent}
                                   :components [{:component :ordinary
                                                 :type key
                                                 :escutcheon (if (= key :escutcheon) :heater nil)
                                                 :field {:content {:tincture (if (= current key) :or :azure)}}}]}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type path key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-ordinary-type [path]
  (let [ordinary-type @(rf/subscribe [:get (conj path :type)])
        names (->> ordinary/choices
                   (map (comp vec reverse))
                   (into {}))]
    [:div.setting
     [:label "Type"]
     " "
     [submenu path "Select Ordinary" (get names ordinary-type) {:min-width "17.5em"}
      (for [[display-name key] ordinary/choices]
        ^{:key key}
        [ordinary-type-choice path key display-name :current ordinary-type])]]))

(defn form-for-ordinary [path & {:keys [parent-field]}]
  (let [ordinary @(rf/subscribe [:get path])]
    [component
     path :ordinary (-> ordinary :type util/translate-cap-first) nil
     [:div.settings
      [form-for-ordinary-type path]
      (let [ordinary-options (ordinary/options ordinary)]
        [:<>
         (when (:escutcheon ordinary-options)
           [form-for-escutcheon (conj path :escutcheon)])
         (when (:line ordinary-options)
           [form-for-line (conj path :line) :options (:line ordinary-options)])
         (when (:opposite-line ordinary-options)
           [form-for-line (conj path :opposite-line)
            :options (:opposite-line ordinary-options)
            :defaults (options/sanitize (:line ordinary) (:line ordinary-options))
            :title "Opposite Line"])
         (when (:diagonal-mode ordinary-options)
           [select (conj path :diagonal-mode) "Diagonal"
            (-> ordinary-options :diagonal-mode :choices)
            :default (-> ordinary-options :diagonal-mode :default)])
         (when (:origin ordinary-options)
           [form-for-position (conj path :origin)
            :title "Origin"
            :options (:origin ordinary-options)])
         (when (:geometry ordinary-options)
           [form-for-geometry (conj path :geometry)
            (:geometry ordinary-options)
            :current (:geometry ordinary)])])
      [checkbox (conj path :hints :outline?) "Outline"]]
     [form-for-field (conj path :field) :parent-field parent-field]]))

(defn form-for-field [path & {:keys [parent-field title-prefix]}]
  (let [division-type (-> @(rf/subscribe [:get path])
                          :division
                          :type
                          (or :none))
        field @(rf/subscribe [:get path])
        counterchanged? (and @(rf/subscribe [:get (conj path :counterchanged?)])
                             (division/counterchangable? (-> parent-field :division)))
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
         :disabled? (not (division/counterchangable? (-> parent-field :division)))])
      (when (not counterchanged?)
        [:<>
         [form-for-division path]
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
         (if (= division-type :none)
           [form-for-content (conj path :content)]
           [checkbox (conj path :division :hints :outline?) "Outline"])])]
     (when (not counterchanged?)
       [:div.parts.components
        [:ul
         (let [content @(rf/subscribe [:get (conj path :division :fields)])
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
                   [form-for-field part-path :title-prefix part-name])]
                [:div {:style {:padding-left "10px"}}
                 (if ref
                   [:a {:on-click #(do (state/dispatch-on-event % [:set part-path (get content ref)])
                                       (state/dispatch-on-event % [:ui-component-open part-path]))}
                    [:i.far.fa-edit]]
                   (when (>= idx mandatory-part-count)
                     [:a {:on-click #(state/dispatch-on-event % [:set (conj part-path :ref)
                                                                 (-> (division/default-fields division-type)
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
                 [form-for-ordinary component-path :parent-field field]
                 [form-for-charge component-path :parent-field field])]
              [:div {:style {:padding-left "10px"}}
               (when (not (and (-> component :component (= :charge))
                               (-> component :type keyword? not)))
                 [:a {:on-click #(state/dispatch-on-event % [:remove-component component-path])}
                  [:i.far.fa-trash-alt]])]])))]]]))
