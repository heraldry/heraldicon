(ns heraldry.frontend.form.component
  (:require [heraldry.coat-of-arms.config :as config]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

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

;; components

(defn checkbox [path label & {:keys [on-change disabled? checked?]}]
  (let [component-id (util/id "checkbox")
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

(defn radio-select [path choices & {:keys [on-change default]}]
  [:div.setting
   (let [current-value (or @(rf/subscribe [:get path])
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
                                   (rf/dispatch [:set path value])))}]
          [:label {:for component-id
                   :style {:margin-right "10px"}} display-name]])))])

(defn selector [path]
  [:a.selector {:on-click #(util/dispatch % [:ui-component-select path])}
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
     [:div.header.clickable {:on-click #(util/dispatch % [:ui-component-open-toggle path])}
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
     [:a {:on-click #(util/dispatch % [:ui-submenu-open submenu-id])}
      link-name]
     (when submenu-open?
       [:div.component.submenu {:style styles}
        [:div.header [:a {:on-click #(util/dispatch % [:ui-submenu-close submenu-id])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))

(defn escutcheon-choice [path key display-name]
  (let [value @(rf/subscribe [:get path])]
    [:div.choice.tooltip {:on-click #(util/dispatch % [:set path key])}
     [:svg {:style {:width "4em"
                    :height "5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        [render/coat-of-arms
         {:escutcheon key
          :field {:component :field
                  :content {:tincture (if (= value key) :or :azure)}}}
         {:outline? true}]]]]
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

(defn form-render-options []
  [component [:render-options] :render-options "Options" nil
   [form-for-escutcheon [:coat-of-arms :escutcheon]]
   (let [path [:render-options :mode]]
     [radio-select path [["Colours" :colours]
                         ["Hatching" :hatching]]
      :default :colours
      :on-change #(let [new-mode %]
                    (rf/dispatch [:set [:render-options :mode] new-mode])
                    (case new-mode
                      :hatching (rf/dispatch [:set [:render-options :outline?] true])
                      :colours (rf/dispatch [:set [:render-options :outline?] false])))])

   [checkbox [:render-options :outline?] "Draw outline"]
   [checkbox [:render-options :squiggly?] "Squiggly lines (can be slow)"]])
