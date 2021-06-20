(ns heraldry.frontend.form.element
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [id]]
            [re-frame.core :as rf]))

(defn checkbox [path label & {:keys [on-change disabled? checked? style]}]
  (let [component-id (id "checkbox")
        checked? (-> (and path
                          @(rf/subscribe [:get path]))
                     (or checked?)
                     boolean
                     (and (not disabled?)))]
    [:div.setting {:style style}
     [:input {:type "checkbox"
              :id component-id
              :checked checked?
              :disabled disabled?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (if on-change
                              (on-change new-checked?)
                              (rf/dispatch [:set path new-checked?])))}]
     [:label {:for component-id} label]]))

(defn keyword->str [k]
  (-> k
      str
      (subs 1)))

(defn select [path label choices & {:keys [value on-change default]}]
  (let [component-id (id "select")
        current-value @(rf/subscribe [:get path])]
    [:div.setting
     [:label {:for component-id} label]
     [:select {:id component-id
               :value (keyword->str (or value
                                        current-value
                                        default
                                        :none))
               :on-change #(let [selected (keyword (-> % .-target .-value))]
                             (if on-change
                               (on-change selected)
                               (rf/dispatch [:set path selected])))}
      (for [[group-name & group-choices] choices]
        (if (and (-> group-choices count (= 1))
                 (-> group-choices first keyword?))
          (let [key (-> group-choices first)]
            ^{:key key}
            [:option {:value (keyword->str key)} group-name])
          ^{:key group-name}
          [:optgroup {:label group-name}
           (for [[display-name key] group-choices]
             ^{:key key}
             [:option {:value (keyword->str key)} display-name])]))]]))

(defn radio-select [path choices & {:keys [on-change default label]}]
  [:div.setting
   (when label
     [:label label])
   [:div (when label
           {:class "other"
            :style {:display "inline-block"}})
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
                    :style {:margin-right "10px"}} display-name]])))]])

(defn range-input [path label min-value max-value & {:keys [value on-change default display-function step
                                                            disabled? tooltip]}]
  (let [component-id (id "range")
        current-value @(rf/subscribe [:get path])
        value (or value
                  current-value
                  default
                  min-value)]
    [:div.setting
     [:label {:for component-id} label]
     (when tooltip
       [:div.tooltip.info {:style {:display "inline-block"
                                   :margin-left "0.2em"}}
        [:i.fas.fa-question-circle]
        [:div.bottom
         [:h3 {:style {:text-align "center"}} tooltip]
         [:i]]])
     [:div.slider
      [:input {:type "range"
               :id component-id
               :min min-value
               :max max-value
               :step step
               :value value
               :disabled disabled?
               :on-change #(let [value (-> % .-target .-value js/parseFloat)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch [:set path value])))}]
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defn range-input-with-checkbox [path label min-value max-value & {:keys [value on-change default display-function step
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
                               (if on-change
                                 (on-change value)
                                 (rf/dispatch [:set path value]))
                               (if on-change
                                 (on-change nil)
                                 (rf/dispatch [:remove path]))))}]
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

(defn text-field [path label & {:keys [on-change default style]}]
  (let [current-value (or @(rf/subscribe [:get path])
                          default)
        input-id (id "input")]
    [:div.setting
     [:label {:for input-id} label]
     [:input {:id input-id
              :type "text"
              :value current-value
              :style style
              :on-change #(let [value (-> % .-target .-value)]
                            (if on-change
                              (on-change value)
                              (rf/dispatch-sync [:set path value])))}]]))
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

(defn ui-submenu [path title link-name styles & content]
  (let [submenu-id (conj path title)
        submenu-open? @(rf/subscribe [:ui-submenu-open? submenu-id])]
    [:div.ui-submenu-setting {:style {:display "inline-block"}
                              :on-click #(.stopPropagation %)}
     [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-open submenu-id])}
      link-name]
     (when submenu-open?
       [:div.ui-component.ui-submenu {:style styles}
        [:div.header [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-close submenu-id])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))

(defn search-field [db-path & {:keys [on-change]}]
  (let [current-value @(rf/subscribe [:get db-path])
        input-id (id "input")]
    [:div {:style {:display "inline-block"
                   :border-radius "999px"
                   :border "1px solid #ccc"
                   :padding "3px 6px"
                   :min-width "10em"
                   :max-width "20em"
                   :width "50%"
                   :margin-bottom "0.5em"}}
     [:i.fas.fa-search]
     [:input {:id input-id
              :name "search"
              :type "text"
              :value current-value
              :autoComplete "off"
              :on-change #(let [value (-> % .-target .-value)]
                            (if on-change
                              (on-change value)
                              (rf/dispatch-sync [:set db-path value])))
              :style {:outline "none"
                      :border "0"
                      :margin-left "0.5em"
                      :width "calc(100% - 12px - 1.5em)"}}]]))
