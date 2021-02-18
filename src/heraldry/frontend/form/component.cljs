(ns heraldry.frontend.form.component
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.charge :as charge]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary :as ordinary]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.frontend.charge :as frontend-charge]
            [heraldry.frontend.charge-map :as charge-map]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [id full-url-for-username]]
            [re-frame.core :as rf]))

(def coa-select-option-context
  (-> context/default
      (dissoc :fn-component-selected?)
      (dissoc :fn-select-component)))

(def ui-render-options-theme-path
  [:ui :render-options :theme])

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
   (-> (loop [db   db
              rest path]
         (if (empty? rest)
           db
           (recur
            (assoc-in db [:ui :component-open? rest] true)
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
 (fn [db [_ path new-type num-fields-x num-fields-y num-base-fields]]
   (if (= new-type :none)
     (-> db
         (update-in path dissoc :division)
         (update-in (conj path :content) #(or % {:tincture :none})))
     (-> db
         (assoc-in (conj path :division :type) new-type)
         (update-in (conj path :division :line :type) #(or % :straight))
         (assoc-in (conj path :division :layout :num-fields-x) num-fields-x)
         (assoc-in (conj path :division :layout :num-fields-y) num-fields-y)
         (assoc-in (conj path :division :layout :num-base-fields) num-base-fields)
         (update-in (conj path :division)
                    (fn [prepared-division]
                      (let [current          (or (:fields prepared-division) [])
                            default          (division/default-fields prepared-division)
                            previous-default (division/default-fields (get-in db (conj path :division)))
                            previous-default (cond
                                               (< (count previous-default) (count default)) (into previous-default (subvec default (count previous-default)))
                                               (> (count previous-default) (count default)) (subvec previous-default 0 (count default))
                                               :else                                        previous-default)
                            merged           (cond
                                               (< (count current) (count default)) (into current (subvec default (count current)))
                                               (> (count current) (count default)) (subvec current 0 (count default))
                                               :else                               current)]
                        (-> prepared-division
                            (assoc :fields (->> (map vector merged previous-default default)
                                                (map (fn [[cur old-def def]]
                                                       (if (and (-> cur :ref not)
                                                                (not= cur old-def))
                                                         cur
                                                         def)))
                                                vec))))))
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
         index           (count (get-in db components-path))]
     {:db (update-in db components-path #(-> %
                                             (conj value)
                                             vec))
      :fx [[:dispatch [:ui-submenu-open (conj components-path index (case (:component value)
                                                                      :ordinary "Select Ordinary"
                                                                      :charge   "Select Charge"))]]
           [:dispatch [:ui-component-open (conj components-path index)]]
           [:dispatch [:ui-component-open (conj components-path index :field)]]]})))

(rf/reg-event-db
 :remove-component
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index           (last path)]
     (update-in db components-path (fn [components]
                                     (vec (concat (subvec components 0 index)
                                                  (subvec components (inc index)))))))))

(rf/reg-event-db
 :move-component-up
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index           (last path)]
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
         index           (last path)]
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

(defn search-field [db-path]
  (let [current-value @(rf/subscribe [:get db-path])
        input-id      (id "input")]
    [:div {:style {:display       "inline-block"
                   :border-radius "999px"
                   :border        "1px solid #ccc"
                   :padding       "3px 6px"
                   :min-width     "10em"
                   :max-width     "20em"
                   :width         "50%"
                   :margin-bottom "0.5em"}}
     [:i.fas.fa-search]
     [:input {:id           input-id
              :name         "search"
              :type         "text"
              :value        current-value
              :autoComplete "off"
              :on-change    #(let [value (-> % .-target .-value)]
                               (rf/dispatch-sync [:set db-path value]))
              :style        {:outline     "none"
                             :border      "0"
                             :margin-left "0.5em"
                             :width       "calc(100% - 12px - 1.5em)"}}]]))

(defn checkbox [path label & {:keys [on-change disabled? checked? style]}]
  (let [component-id (id "checkbox")
        checked?     (-> (and path
                              @(rf/subscribe [:get path]))
                         (or checked?)
                         boolean
                         (and (not disabled?)))]
    [:div.setting {:style style}
     [:input {:type      "checkbox"
              :id        component-id
              :checked   checked?
              :disabled  disabled?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (if on-change
                              (on-change new-checked?)
                              (rf/dispatch [:set path new-checked?])))}]
     [:label {:for component-id} label]]))

(defn select [path label choices & {:keys [grouped? value on-change default]}]
  (let [component-id  (id "select")
        current-value @(rf/subscribe [:get path])]
    [:div.setting
     [:label {:for component-id} label]
     [:select {:id        component-id
               :value     (name (or value
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
          [:input {:id        component-id
                   :type      "radio"
                   :value     (name key)
                   :checked   (= key current-value)
                   :on-change #(let [value (keyword (-> % .-target .-value))]
                                 (if on-change
                                   (on-change value)
                                   (rf/dispatch [:set path value])))}]
          [:label {:for   component-id
                   :style {:margin-right "10px"}} display-name]])))])

(defn range-input [path label min-value max-value & {:keys [value on-change default display-function step
                                                            disabled?]}]
  (let [component-id  (id "range")
        current-value @(rf/subscribe [:get path])
        value         (or value
                          current-value
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
                               (rf/dispatch [:set path value])))}]
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defn range-input-with-checkbox [path label min-value max-value & {:keys [value on-change default display-function step
                                                                          disabled?]}]
  (let [component-id   (id "range")
        checkbox-id    (id "checkbox")
        current-value  @(rf/subscribe [:get path])
        value          (or value
                           current-value
                           default
                           min-value)
        using-default? (nil? current-value)
        checked?       (not using-default?)]
    [:div.setting
     [:label {:for component-id} label]
     [:div.slider
      [:input {:type      "checkbox"
               :id        checkbox-id
               :checked   checked?
               :disabled? disabled?
               :on-change #(let [new-checked? (-> % .-target .-checked)]
                             (if new-checked?
                               (if on-change
                                 (on-change value)
                                 (rf/dispatch [:set path value]))
                               (if on-change
                                 (on-change nil)
                                 (rf/dispatch [:remove path]))))}]
      [:input {:type      "range"
               :id        component-id
               :min       min-value
               :max       max-value
               :step      step
               :value     value
               :disabled  (or disabled?
                              using-default?)
               :on-change #(let [value (-> % .-target .-value js/parseFloat)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch [:set path value])))}]
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defn text-field [path label & {:keys [on-change default]}]
  (let [current-value (or @(rf/subscribe [:get path])
                          default)
        input-id      (id "input")]
    [:div.setting
     [:label {:for input-id} label]
     [:input {:id        input-id
              :type      "text"
              :value     current-value
              :on-change #(let [value (-> % .-target .-value)]
                            (if on-change
                              (on-change value)
                              (rf/dispatch-sync [:set path value])))}]]))

(defn selector [path]
  [:a.selector {:on-click #(state/dispatch-on-event % [:ui-component-select path])}
   [:i.fas.fa-search]])

(defn component [path type title title-prefix & content]
  (let [selected?      @(rf/subscribe [:ui-component-selected? path])
        content?       (seq content)
        open?          (and @(rf/subscribe [:ui-component-open? path])
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
                                         (-> #{:charge :ordinary}
                                             (get type)))
                                (str (util/translate-cap-first type) ":"))
                              title])]
      (when show-selector?
        [selector path])]
     (when (and open?
                content?)
       (into [:div.content]
             content))]))

(defn submenu [path title link-name styles & content]
  (let [submenu-id    (conj path title)
        submenu-open? @(rf/subscribe [:ui-submenu-open? submenu-id])]
    [:div.submenu-setting {:style    {:display "inline-block"}
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
  (let [value            @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          (if (= key :none)
                            {:escutcheon :rectangle
                             :field      {:component :field
                                          :content   {:tincture :void}}}
                            {:escutcheon key
                             :field      {:component :field
                                          :content   {:tincture (if (= value key) :or :azure)}}})
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style               {:width  "4em"
                                  :height "5em"}
            :viewBox             "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-escutcheon [path label & {:keys [label-width allow-none? choices]}]
  (let [escutcheon (or @(rf/subscribe [:get path])
                       (when allow-none?
                         :none)
                       (when choices
                         (-> choices first second))
                       :heater)
        choices    (or choices
                       (if allow-none?
                         (concat [["None" :none]]
                                 escutcheon/choices)
                         escutcheon/choices))
        names      (->> choices
                        (map (comp vec reverse))
                        (into {}))]
    [:div.setting
     [:label label]
     " "
     (conj (if label-width
             [:div {:style {:display  "inline-block"
                            :position "absolute"
                            :left     label-width}}]
             [:<>])
           [submenu path "Select Escutcheon" (get names escutcheon) {:min-width "17.5em"}
            (for [[display-name key] choices]
              ^{:key key}
              [escutcheon-choice path key display-name])])
     [:div.spacer]]))

(defn form-render-options [db-path]
  [component db-path :render-options "Options" nil
   (let [mode-path    (conj db-path :mode)
         outline-path (conj db-path :outline?)]
     [:<>
      [form-for-escutcheon (conj db-path :escutcheon-override) "Escutcheon Override"
       :label-width "11em"
       :allow-none? true]
      [radio-select mode-path [["Colours" :colours]
                               ["Hatching" :hatching]]
       :default :colours
       :on-change #(let [new-mode %]
                     (rf/dispatch [:set mode-path new-mode])
                     (case new-mode
                       :hatching (rf/dispatch [:set outline-path true])
                       :colours  (rf/dispatch [:set outline-path false])))]
      (when (= @(rf/subscribe [:get mode-path]) :colours)
        [select (conj db-path :theme) "Colour Theme" tincture/theme-choices
         :on-change #(do
                       (rf/dispatch [:set (conj db-path :theme) %])
                       (rf/dispatch [:set ui-render-options-theme-path %]))
         :grouped? true
         :default tincture/default-theme])])
   [checkbox (conj db-path :outline?) "Draw outline"]
   [checkbox (conj db-path :squiggly?) "Squiggly lines (can be slow)"]])

(defn form-for-coat-of-arms [db-path]
  [component db-path :coat-of-arms "Coat of Arms" nil
   [form-for-escutcheon (conj db-path :escutcheon) "Default Escutcheon" :label-width "11em"]
   [form-for-field (conj db-path :field)]])

(defn tincture-choice [path key display-name]
  (let [value            @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field      {:component :field
                                        :content   {:tincture key}}}
                          40
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                          :style    {:border        (if (= value key)
                                                      "1px solid #000"
                                                      "1px solid transparent")
                                     :border-radius "5px"}}
     [:svg {:style               {:width  "4em"
                                  :height "4.5em"}
            :viewBox             "0 0 50 100"
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
        names (-> tincture/tincture-map
                  (assoc :none "None"))]
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
           [:h4  group-name])
         (for [[display-name key] group]
           ^{:key display-name}
           [tincture-choice path key display-name])])]]))

(defn form-for-position [path & {:keys [title options] :or {title "Position"}}]
  (let [position      @(rf/subscribe [:get path])
        point-path    (conj path :point)
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
        [range-input-with-checkbox offset-x-path "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :default (options/get-value (:offset-x position) (:offset-x options))
         :display-function #(str % "%")])
      (when (:offset-y options)
        [range-input-with-checkbox offset-y-path "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :default (options/get-value (:offset-y position) (:offset-y options))
         :display-function #(str % "%")])]]))

(defn form-for-geometry [path options & {:keys [current]}]
  (let [changes         (filter some? [(when (and (:size current)
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
         [range-input-with-checkbox (conj path :size) "Size"
          (-> options :size :min)
          (-> options :size :max)
          :default (options/get-value (:size current) (:size options))
          :display-function #(str % "%")])
       (when (:stretch options)
         [range-input-with-checkbox (conj path :stretch) "Stretch"
          (-> options :stretch :min)
          (-> options :stretch :max)
          :step 0.01
          :default (options/get-value (:stretch current) (:stretch options))])
       (when (:rotation options)
         [range-input-with-checkbox (conj path :rotation) "Rotation"
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
                           :field      {:component  :field
                                        :content    {:tincture :argent}
                                        :components [{:component  :charge
                                                      :type       key
                                                      :geometry   {:size 75}
                                                      :escutcheon (if (= key :escutcheon) :heater nil)
                                                      :field      {:content {:tincture (if (= current key) :or :azure)}}}]}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:update-charge path {:type     key
                                                                                      :attitude nil
                                                                                      :facing   nil
                                                                                      :data     nil
                                                                                      :variant  nil}])}
     [:svg {:style               {:width  "4em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 200"
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
                           :field      {:component  :field
                                        :content    {:tincture :argent}
                                        :components [{:component :charge
                                                      :type      (:type charge)
                                                      :variant   (:variant charge)
                                                      :field     {:content {:tincture :or}}}]}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip
     [:svg {:style               {:width  "4em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(def node-icons
  {:group    {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :attitude {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :facing   {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :charge   {:closed "fa-plus-square"
              :open   "fa-minus-square"}
   :variant  {:normal "fa-image"}})

(defn tree-for-charge-map [{:keys [node-type name groups charges attitudes facings variants] :as node}
                           tree-path
                           selected-charge remaining-path-to-charge
                           {:keys [still-on-path? render-variant open-all?]
                            :as   opts}]
  (let [flag-path     (conj [:ui :charge-map] tree-path)
        db-open?-path @(rf/subscribe [:get flag-path])
        open?         (or open-all?
                          (= node-type :_root)
                          (and (nil? db-open?-path)
                               still-on-path?)
                          db-open?-path)
        variant?      (= node-type :variant)]
    (cond-> [:<>]
      variant?            (conj
                           [:div.node-name {:on-click nil
                                            :style    {:color (when still-on-path? "#1b6690")
                                                       :left  0}}
                            "\u2022 " [render-variant node]])
      (and (not variant?)
           (not= node-type
                 :_root)) (conj
                           [:div.node-name.clickable
                            {:on-click #(state/dispatch-on-event % [:toggle flag-path])
                             :style    {:color (when still-on-path? "#1b6690")}}
                            (if open?
                              [:i.far {:class (-> node-icons (get node-type) :open)}]
                              [:i.far {:class (-> node-icons (get node-type) :closed)}])
                            [:<>
                             [(cond
                                (and (= node-type :variant)
                                     still-on-path?)    :b
                                (= node-type :charge)   :b
                                (= node-type :attitude) :em
                                (= node-type :facing)   :em
                                :else                   :<>) name]
                             (let [c (charge-map/count-variants node)]
                               (when (pos? c)
                                 [:span.count-badge c]))]])
      (and open?
           groups)        (conj [:ul
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
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           charges)       (conj [:ul
                                 (for [[key charge] (sort-by first charges)]
                                   (let [following-path? (and still-on-path?
                                                              (-> remaining-path-to-charge
                                                                  count zero?)
                                                              (= (:type charge)
                                                                 (:type selected-charge)))]
                                     ^{:key key}
                                     [:li.charge
                                      [tree-for-charge-map
                                       charge
                                       (conj tree-path :charges key)
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           attitudes)     (conj [:ul
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
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           facings)       (conj [:ul
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
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))])
      (and open?
           variants)      (conj [:ul
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
                                       selected-charge
                                       remaining-path-to-charge
                                       (-> opts
                                           (assoc :still-on-path? following-path?))]]))]))))

(defn charge-properties [charge]
  [:div.properties {:style {:display        "inline-block"
                            :line-height    "1.5em"
                            :vertical-align "middle"
                            :white-space    "normal"}}
   (when (-> charge :is-public not)
     [:div.tag.private [:i.fas.fa-lock] "private"])
   (when-let [attitude (-> charge
                           :attitude
                           (#(when (not= % :none) %)))]
     [:div.tag.attitude (util/translate attitude)])
   (when-let [facing (-> charge
                         :facing
                         (#(when (-> % #{:none :to-dexter} not) %)))]
     [:div.tag.facing (util/translate facing)])
   (for [attribute (->> charge
                        :attributes
                        (filter second)
                        (map first)
                        sort)]
     ^{:key attribute}
     [:div.tag.attribute (util/translate attribute)])
   (when-let [fixed-tincture (-> charge
                                 :fixed-tincture
                                 (or :none)
                                 (#(when (not= % :none) %)))]
     [:div.tag.fixed-tincture (util/translate fixed-tincture)])
   (for [modifier (->> charge
                       :colours
                       (map second)
                       (filter #(-> %
                                    #{:primary
                                      :keep
                                      :outline
                                      :eyes-and-teeth}
                                    not))
                       sort)]
     ^{:key modifier}
     [:div.tag.modifier (util/translate modifier)])])

(defn matches-word [data word]
  (cond
    (keyword? data) (-> data name s/lower-case (s/includes? word))
    (string? data)  (-> data s/lower-case (s/includes? word))
    (map? data)     (some (fn [[k v]]
                            (or (and (keyword? k)
                                     (matches-word k word)
                                     ;; this would be an attribute entry, the value
                                     ;; must be truthy as well
                                     v)
                                (matches-word v word))) data)))

(defn filter-charges [charges filter-string]
  (if (or (not filter-string)
          (-> filter-string s/trim count zero?))
    charges
    (let [words (-> filter-string
                    (s/split #" +")
                    (->> (map s/lower-case)))]
      (filterv (fn [charge]
                 (every? (fn [word]
                           (some (fn [attribute]
                                   (-> charge
                                       (get attribute)
                                       (matches-word word)))
                                 [:name :type :attitude :facing :attributes :colours :username]))
                         words))
               charges))))

(defn charge-tree [charges & {:keys [remove-empty-groups? hide-access-filters?
                                     link-to-charge render-variant refresh-action]}]
  [:div.tree
   (let [user-data            (user/data)
         filter-db-path       [:ui :charge-tree :filter-string]
         show-public-db-path  [:ui :charge-tree :show-public?]
         show-own-db-path     [:ui :charge-tree :show-own?]
         show-public?         @(rf/subscribe [:get show-public-db-path])
         show-own?            @(rf/subscribe [:get show-own-db-path])
         filter-string        @(rf/subscribe [:get filter-db-path])
         filtered-charges     (-> charges
                                  (filter-charges filter-string)
                                  (cond->>
                                   (not hide-access-filters?) (filter (fn [charge]
                                                                        (or (and show-public?
                                                                                 (:is-public charge))
                                                                            (and show-own?
                                                                                 (= (:username charge)
                                                                                    (:username user-data))))))))
         filtered?            (or (and (not hide-access-filters?)
                                       (not show-public?))
                                  (-> filter-string count pos?))
         remove-empty-groups? (or remove-empty-groups?
                                  filtered?)
         open-all?            filtered?
         charge-map           (charge-map/build-charge-map
                               filtered-charges
                               :remove-empty-groups? remove-empty-groups?)]
     [:<>
      [search-field filter-db-path]
      (when refresh-action
        [:a {:style    {:margin-left "0.5em"}
             :on-click #(do
                          (refresh-action)
                          (.stopPropagation %))} [:i.fas.fa-sync-alt]])
      (when (not hide-access-filters?)
        [:div
         [checkbox show-public-db-path "Public charges" :style {:display "inline-block"}]
         [checkbox show-own-db-path "Own charges" :style {:display     "inline-block"
                                                          :margin-left "1em"}]])
      (if (empty? filtered-charges)
        [:div "None"]
        [tree-for-charge-map charge-map [] nil nil
         {:open-all?      open-all?
          :render-variant (or render-variant
                              (fn [node]
                                (let [charge   (-> node :data)
                                      username (-> charge :username)]
                                  [:div {:style {:display        "inline-block"
                                                 :white-space    "normal"
                                                 :vertical-align "top"
                                                 :line-height    "1.5em"}}
                                   [:div {:style {:display        "inline-block"
                                                  :vertical-align "top"}}
                                    [link-to-charge (-> node :data)]
                                    " by "
                                    [:a {:href   (full-url-for-username username)
                                         :target "_blank"} username]]
                                   [charge-properties charge]])))}])])])

(defn form-for-charge-type [path]
  (let [charge      @(rf/subscribe [:get path])
        charge-type (:type charge)
        names       (->> charge/choices
                         (map (comp vec reverse))
                         (into {}))
        title       (util/combine " " [(or (get names charge-type)
                                           (-> charge :type util/translate-cap-first))
                                       (-> charge :attitude util/translate)
                                       (-> charge :facing util/translate)])]
    [:div.setting
     [:label "Type"]
     " "
     [submenu path "Select Charge" title {:min-width "22em"}
      (for [[display-name key] charge/choices]
        ^{:key key}
        [charge-type-choice path key display-name :current charge-type])
      (when (-> names (contains? charge-type) not)
        [charge-type-selected-choice charge title])
      (let [[status charges] (state/async-fetch-data
                              [:all-charges]
                              :all-charges
                              frontend-charge/fetch-charges)]
        [:div {:style {:padding "15px"}}
         (if (= status :done)
           [charge-tree charges
            :refresh-action #(state/invalidate-cache [:all-charges] :all-charges)
            :render-variant (fn [node]
                              (let [charge-data (:data node)
                                    username    (:username charge-data)]
                                [:div {:style {:display        "inline-block"
                                               :white-space    "normal"
                                               :vertical-align "top"
                                               :line-height    "1.5em"}}
                                 [:div {:style {:display        "inline-block"
                                                :vertical-align "top"}}
                                  [:a.clickable
                                   {:on-click #(state/dispatch-on-event
                                                %
                                                [:update-charge
                                                 path
                                                 (merge {:type    (:type charge-data)
                                                         :variant {:id      (:id charge-data)
                                                                   :version (:latest-version charge-data)}}
                                                        (select-keys charge-data
                                                                     [:attitude :facing]))])}
                                   (:name charge-data)]
                                  " by "
                                  [:a {:href   (full-url-for-username username)
                                       :target "_blank"} username]]
                                 [charge-properties charge-data]]))]
           [:div "loading..."])])]]))

(defn form-for-charge [path & {:keys [parent-field]}]
  (let [charge                     @(rf/subscribe [:get path])
        charge-data                (when-let [variant (:variant charge)]
                                     (frontend-charge/fetch-charge-data variant))
        fixed-tincture             (-> charge-data
                                       :fixed-tincture
                                       (or :none))
        supported-tinctures        (-> attributes/tincture-modifier-map
                                       keys
                                       set
                                       (conj :eyes-and-teeth)
                                       (set/intersection
                                        (-> charge-data
                                            :colours
                                            (->> (map second))
                                            set)))
        sorted-supported-tinctures (-> supported-tinctures
                                       (disj :eyes-and-teeth)
                                       sort
                                       vec)
        tinctures-set              (-> charge
                                       :tincture
                                       (->> (filter (fn [[_ v]]
                                                      (and (some? v)
                                                           (not= v :none))))
                                            (map first)
                                            set)
                                       (filter supported-tinctures)
                                       (->> (map util/translate-cap-first)))
        tinctures-title            (if (-> tinctures-set count pos?)
                                     (util/combine ", " tinctures-set)
                                     "Default")
        tinctures-title            (if (-> tinctures-title count (> 30))
                                     (str (subs tinctures-title 0 27) "...")
                                     tinctures-title)
        title                      (s/join " " [(-> charge :type util/translate-cap-first)
                                                (-> charge :attitude util/translate)])]
    [component path :charge title nil
     [:div.settings
      (when (and (:type charge)
                 (-> charge :type :map? not))
        [form-for-charge-type path])
      (when (-> supported-tinctures
                count
                pos?)
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
           (when (get supported-tinctures :eyes-and-teeth)
             [checkbox
              (conj path :tincture :eyes-and-teeth)
              "White eyes and teeth"
              :on-change #(rf/dispatch [:set
                                        (conj path :tincture :eyes-and-teeth)
                                        (if % :argent nil)])])]
          [:div.spacer]]])
      (let [charge-options (charge/options charge)]
        [:<>
         (when (:position charge-options)
           [form-for-position (conj path :position)
            :title "Position"
            :options (:position charge-options)])
         (when (:geometry charge-options)
           [form-for-geometry (conj path :geometry)
            (:geometry charge-options)
            :current (:geometry charge)])
         (when (:escutcheon charge-options)
           [form-for-escutcheon (conj path :escutcheon) "Escutcheon"
            :choices (-> charge-options :escutcheon :choices)])])
      [select (conj path :hints :outline-mode) "Outline" [["Keep" :keep]
                                                          ["Remove" :remove]
                                                          ["Primary" :primary]
                                                          ["Transparent" :transparent]]
       :default :keep]]
     (if (not= fixed-tincture :none)
       [:div {:style {:margin-bottom "0.5em"}}
        "Fixed tincture:" [:span.tag.fixed-tincture fixed-tincture]]
       [form-for-field (conj path :field) :parent-field parent-field])]))

(defn division-choice [path key display-name]
  (let [division         (-> @(rf/subscribe [:get path])
                             :division)
        value            (-> division
                             :type
                             (or :none))
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field      (if (= key :none)
                                         {:component :field
                                          :content   {:tincture (if (= value key) :or :azure)}}
                                         {:component :field
                                          :division  {:type   key
                                                      :fields (-> (division/default-fields {:type key})
                                                                  (util/replace-recursively :none :argent)
                                                                  (cond->
                                                                   (= value key) (util/replace-recursively :azure :or)))
                                                      :layout {:num-fields-y (when (= key :chequy)
                                                                               7)}}})}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(let [new-division              (assoc division :type key)
                                           {:keys [num-fields-x
                                                   num-fields-y
                                                   num-base-fields]} (:layout (options/sanitize-or-nil
                                                                               new-division
                                                                               (division/options new-division)))]
                                       (state/dispatch-on-event % [:set-division-type path key num-fields-x num-fields-y num-base-fields]))}
     [:svg {:style               {:width  "4em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 200"
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
        names         (->> (into [["None" :none]]
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
  (let [options          (line/options {:type key})
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :flag
                           :field      {:component :field
                                        :division  {:type   :per-fess
                                                    :line   {:type  key
                                                             :width (* 2 (options/get-value nil (:width options)))}
                                                    :fields [{:content {:tincture :argent}}
                                                             {:content {:tincture (if (= key current) :or :azure)}}]}}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style               {:width  "6.5em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 80"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-line-type [path & {:keys [options can-disable? default value]}]
  (let [line  @(rf/subscribe [:get path])
        value (or value
                  (options/get-value (:type line) (:type options)))]
    [:div.setting
     [:label "Type"]
     [:div.other {:style {:display "inline-block"}}
      (when can-disable?
        [:input {:type      "checkbox"
                 :checked   (some? (:type line))
                 :on-change #(let [new-checked? (-> % .-target .-checked)]
                               (if new-checked?
                                 (rf/dispatch [:set (conj path :type) default])
                                 (rf/dispatch [:remove (conj path :type)])))}])
      (if (some? (:type line))
        [submenu (conj path :type) "Select Line Type" (get line/line-map value) {:min-width "25em"}
         (for [[display-name key] (-> options :type :choices)]
           ^{:key display-name}
           [line-type-choice (conj path :type) key display-name :current value])]
        (when can-disable?
          [:span {:style {:color "#ccc"}} (get line/line-map value)
           " (inherited)"]))]]))

(defn form-for-line [path & {:keys [title options defaults] :or {title "Line"}}]
  (let [line              @(rf/subscribe [:get path])
        line-type         (or (:type line)
                              (:type defaults))
        line-eccentricity (or (:eccentricity line)
                              (:eccentricity defaults))
        line-height       (or (:height line)
                              (:height defaults))
        line-width        (or (:width line)
                              (:width defaults))
        line-offset       (or (:offset line)
                              (:offset defaults))]
    [:div.setting
     [:label title]
     " "
     [submenu path title (get line/line-map line-type) {}
      [form-for-line-type path :options options
       :can-disable? (some? defaults)
       :value line-type
       :default (:type defaults)]
      (when (:eccentricity options)
        [range-input-with-checkbox (conj path :eccentricity) "Eccentricity"
         (-> options :eccentricity :min)
         (-> options :eccentricity :max)
         :step 0.01
         :default (or (:eccentricity defaults)
                      (options/get-value line-eccentricity (:eccentricity options)))])
      (when (:height options)
        [range-input-with-checkbox (conj path :height) "Height"
         (-> options :height :min)
         (-> options :height :max)
         :step 0.01
         :default (or (:height defaults)
                      (options/get-value line-height (:height options)))])
      (when (:width options)
        [range-input-with-checkbox (conj path :width) "Width"
         (-> options :width :min)
         (-> options :width :max)
         :default (or (:width defaults)
                      (options/get-value line-width (:width options)))
         :display-function #(str % "%")])
      (when (:offset options)
        [range-input-with-checkbox (conj path :offset) "Offset"
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
                           :field      {:component  :field
                                        :content    {:tincture :argent}
                                        :components [{:component  :ordinary
                                                      :type       key
                                                      :escutcheon (if (= key :escutcheon) :heater nil)
                                                      :field      {:content {:tincture (if (= current key) :or :azure)}}}]}}
                          100
                          (-> coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type path key])}
     [:svg {:style               {:width  "4em"
                                  :height "4.5em"}
            :viewBox             "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form-for-ordinary-type [path]
  (let [ordinary-type @(rf/subscribe [:get (conj path :type)])]
    [:div.setting
     [:label "Type"]
     " "
     [submenu path "Select Ordinary" (get ordinary/ordinary-map ordinary-type) {:min-width "17.5em"}
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

(defn form-for-layout [field-path & {:keys [title options] :or {title "Layout"}}]
  (let [layout-path    (conj field-path :division :layout)
        division       @(rf/subscribe [:get (conj field-path :division)])
        layout         (:layout division)
        division-type  (:type division)
        current-data   (:layout (options/sanitize-or-nil division (division/options division)))
        effective-data (:layout (options/sanitize division (division/options division)))
        link-name      (util/combine
                        ", "
                        [(cond
                           (= division-type :paly)            (str (:num-fields-x effective-data) " fields")
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
                           (str "stretched"))])
        link-name      (if (-> link-name count (= 0))
                         "Default"
                         link-name)]
    [:div.setting
     [:label title]
     " "
     [submenu layout-path title link-name {}
      (when (-> options :num-base-fields)
        [range-input-with-checkbox (conj layout-path :num-base-fields) "Base fields"
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
        [range-input-with-checkbox (conj layout-path :num-fields-x) "x-Subfields"
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
        [range-input-with-checkbox (conj layout-path :num-fields-y) "y-Subfields"
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
        [range-input-with-checkbox (conj layout-path :offset-x) "Offset x"
         (-> options :offset-x :min)
         (-> options :offset-x :max)
         :step 0.01
         :default (options/get-value (:offset-x layout) (:offset-x options))])
      (when (-> options :offset-y)
        [range-input-with-checkbox (conj layout-path :offset-y) "Offset y"
         (-> options :offset-y :min)
         (-> options :offset-y :max)
         :step 0.01
         :default (options/get-value (:offset-y layout) (:offset-y options))])
      (when (-> options :stretch-x)
        [range-input-with-checkbox (conj layout-path :stretch-x) "Stretch x"
         (-> options :stretch-x :min)
         (-> options :stretch-x :max)
         :step 0.01
         :default (options/get-value (:stretch-x layout) (:stretch-x options))])
      (when (-> options :stretch-y)
        [range-input-with-checkbox (conj layout-path :stretch-y) "Stretch y"
         (-> options :stretch-y :min)
         (-> options :stretch-y :max)
         :step 0.01
         :default (options/get-value (:stretch-y layout) (:stretch-y options))])
      (when (-> options :rotation)
        [range-input-with-checkbox (conj layout-path :rotation) "Rotation"
         (-> options :rotation :min)
         (-> options :rotation :max)
         :step 5
         :default (options/get-value (:rotation layout) (:rotation options))])]]))

(defn form-for-field [path & {:keys [parent-field title-prefix]}]
  (let [division        (-> @(rf/subscribe [:get path])
                            :division)
        division-type   (-> division
                            :type
                            (or :none))
        field           @(rf/subscribe [:get path])
        counterchanged? (and @(rf/subscribe [:get (conj path :counterchanged?)])
                             (division/counterchangable? (-> parent-field :division)))
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
        [checkbox (conj path :counterchanged?) "Counterchanged"
         :disabled? (not (division/counterchangable? (-> parent-field :division)))])
      (when (not counterchanged?)
        [:<>
         [form-for-division path]
         (let [division-options (division/options (:division field))]
           [:<>
            (when (-> division-options :origin)
              [form-for-position (conj path :division :origin)
               :title "Origin"
               :options (:origin division-options)])
            (when (-> division-options :diagonal-mode)
              [select (conj path :division :diagonal-mode) "Diagonal"
               (-> division-options :diagonal-mode :choices)
               :default (-> division-options :diagonal-mode :default)])
            (when (:layout division-options)
              [form-for-layout path :options (:layout division-options)])
            (when (:line division-options)
              [form-for-line (conj path :division :line) :options (:line division-options)])])
         (if (= division-type :none)
           [form-for-content (conj path :content)]
           [checkbox (conj path :division :hints :outline?) "Outline"])])]
     (cond
       (#{:chequy
          :lozengy} division-type) [:div.parts.components {:style {:margin-bottom "0.5em"}}
                                    [:ul
                                     (let [tinctures @(rf/subscribe [:get (conj path :division :fields)])]
                                       (for [idx (range (count tinctures))]
                                         ^{:key idx}
                                         [:li
                                          [form-for-tincture (conj path :division :fields idx :content :tincture)
                                           :label (str "Tincture " (inc idx))]]))]]
       (not counterchanged?)       [:div.parts.components
                                    [:ul
                                     (let [content              @(rf/subscribe [:get (conj path :division :fields)])
                                           mandatory-part-count (division/mandatory-part-count division)]
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
                             :white-space   "nowrap"}}
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

(defn form-attribution [db-path]
  (let [attribution-options [["None (No sharing)" :none]
                             ["CC Attribution" :cc-attribution]
                             ["CC Attribution-ShareAlike" :cc-attribution-share-alike]
                             ["Public Domain" :public-domain]]
        license-nature      @(rf/subscribe [:get (conj db-path :nature)])]
    [component db-path :attribution "Attribution / License" nil
     [select (conj db-path :license) "License" attribution-options]
     [radio-select (conj db-path :nature) [["Own work" :own-work]
                                           ["Derivative" :derivative]]
      :default :own-work]
     (when (= license-nature :derivative)
       [:<>
        [select (conj db-path :source-license) "Source license" (assoc-in
                                                                 attribution-options
                                                                 [0 0]
                                                                 "None")]
        [text-field (conj db-path :source-name) "Source name"]
        [text-field (conj db-path :source-link) "Source link"]
        [text-field (conj db-path :source-creator-name) "Creator name"]
        [text-field (conj db-path :source-creator-link) "Creator link"]])
     [:div {:style {:margin-bottom "1em"}} " "]]))
