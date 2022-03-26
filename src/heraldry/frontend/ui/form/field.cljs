(ns heraldry.frontend.ui.form.field
  (:require
   ["genex" :as genex]
   ["html-entities" :as html-entities]
   ["react-contenteditable" :as ContentEditable]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldry.blazonry.parser :as blazonry-parser]
   [heraldry.coat-of-arms.default :as default]
   [heraldry.coat-of-arms.field.core :as field]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.modal :as modal]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.tincture-select :as tincture-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.frontend.validation :as validation]
   [heraldry.interface :as interface]
   [heraldry.static :as static]
   [heraldry.util :as util]
   [hiccups.runtime]
   [re-frame.core :as rf]
   [reagent.core :as r])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))

(macros/reg-event-db :override-field-part-reference
  (fn [db [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      (-> db
          (assoc-in path referenced-part)
          (state/ui-component-node-select path :open? true)))))

(macros/reg-event-db :reset-field-part-reference
  (fn [db [_ {:keys [path] :as context}]]
    (let [index (last path)
          parent-context (c/-- context 2)]
      (assoc-in db path (-> (field/default-fields parent-context)
                            (get index))))))

(defn serialize-style [style]
  (->> style
       (map (fn [[k v]]
              (str (name k) ":" v ";")))
       (apply str)))

(defn serialize-styles [data]
  (walk/postwalk (fn [v]
                   (if (and (map? v)
                            (:style v))
                     (assoc v :style (serialize-style (:style v)))
                     v))
                 data))

(defn caret-position [index & {:keys [parent-offset]}]
  (let [parent-offset-top (-> parent-offset :top (or 0))
        parent-offset-left (-> parent-offset :left (or 0))
        selection (js/document.getSelection)
        range (.getRangeAt selection 0)
        node (.-startContainer range)
        offset (or (min index (.-length node))
                   (.-startOffset range))]
    (cond
      (pos? offset) (let [rect (-> (doto (js/document.createRange)
                                     (.setStart node (dec offset))
                                     (.setEnd node offset))
                                   .getBoundingClientRect)]
                      {:top (-> rect
                                .-top
                                (- parent-offset-top))
                       :left (-> rect
                                 .-left
                                 (- parent-offset-left))})
      (< offset (.-length node)) (let [rect (-> (doto (js/document.createRange)
                                                  (.setStart node offset)
                                                  (.setEnd node (inc offset)))
                                                .getBoundingClientRect)]
                                   {:top (-> rect
                                             .-top
                                             (- parent-offset-top))
                                    :left (-> rect
                                              .-left
                                              (- parent-offset-left))})
      :else (let [rect (.getBoundingClientRect node)
                  styles (js/getComputedStyle node)
                  line-height (js/parseInt (.-lineHeight styles))
                  font-size (js/parseInt (.-lineSize styles))
                  delta (/ (- line-height font-size) 2)]
              {:top (-> rect
                        .-top
                        (+ delta)
                        (- parent-offset-top))
               :left (-> rect
                         .-left
                         (- parent-offset-left))}))))

(defn modal-position [class]
  (if-let [element (first (js/document.getElementsByClassName class))]
    (let [styles (js/getComputedStyle element)]
      {:top (-> styles
                .-top
                js/parseInt
                (- (/ (-> styles .-width js/parseInt) 2)))
       :left (-> styles
                 .-left
                 js/parseInt
                 (- (/ (-> styles .-height js/parseInt) 2)))})
    {:top 0
     :left 0}))

(rf/reg-sub :get-blazon-data
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (try
      (let [hdn (blazonry-parser/blazon->hdn value)]
        {:value value
         :html value
         :hdn hdn})
      (catch :default e
        (let [{:keys [reason index]} (ex-data e)
              parsed (subs value 0 index)
              problem (subs value index)
              auto-complete-choices (->> reason
                                         (mapcat (fn [{:keys [tag expecting]}]
                                                   (case tag
                                                     :string [expecting]
                                                     :regexp (-> expecting
                                                                 genex
                                                                 .generate
                                                                 js->clj))))
                                         vec)
              modal-pos (modal-position "modal")]
          {:value value
           :html [:span (html-entities/encode parsed)
                  [:span {:style {:color "red"}} (html-entities/encode problem)]]
           :auto-complete-choices auto-complete-choices
           :auto-complete-position (caret-position index :parent-offset modal-pos)
           :index index})))))

(defn strip-html-tags [value]
  (-> value
      (s/replace #"<ul.*?</ul>" "")
      (s/replace #"<.*?>" "")))

(macros/reg-event-db :set-blazon-data
  (fn [db [_ path value]]
    (assoc-in db path (-> value
                          strip-html-tags
                          (s/replace "&nbsp;" " ")
                          html-entities/decode))))

(def content-editable (r/adapt-react-class ContentEditable/default))

(macros/reg-event-db :from-blazon
  (fn [_ [_ context]]
    (modal/create
     :string.button/from-blazon
     [(fn []
        (let [content-path [:ui :blazon-editor]
              {:keys [auto-complete-choices
                      auto-complete-position]
               :as data} @(rf/subscribe [:get-blazon-data content-path])]
          [:div {:style {}}
           [content-editable
            {:style {:outline "1px solid black"
                     :width "15em"
                     :height "10em"}
             :html (-> data :html serialize-styles html)
             :on-change #(let [value (-> % .-target .-value)]
                           (rf/dispatch-sync [:set-blazon-data content-path value]))}]
           (into [:ul.auto-complete-box {:style {:top (-> auto-complete-position :top)
                                                 :left (-> auto-complete-position :left)}}]
                 (map (fn [choice]
                        [:li choice]))
                 auto-complete-choices)]))])))

(defn show-tinctures-only? [field-type]
  (-> field-type name keyword
      #{:chequy
        :lozengy
        :vairy
        :potenty
        :masony
        :papellony
        :fretty}))

(defn form [context]
  [:<>
   (ui-interface/form-elements
    context
    [:inherit-environment?
     :type
     :tincture
     :pattern-scaling
     :pattern-rotation
     :pattern-offset-x
     :pattern-offset-y
     :line
     :opposite-line
     :extra-line
     :variant
     :thickness
     :gap
     :origin
     :direction-anchor
     :anchor
     :geometry
     :layout
     :outline?
     :manual-blazon])

   (when (show-tinctures-only? (interface/get-raw-data (c/++ context :type)))
     [:<>
      [:div {:style {:margin-bottom "1em"}}]
      (for [idx (range (interface/get-list-size (c/++ context :fields)))]
        ^{:key idx}
        [:<>
         [tincture-select/tincture-select (c/++ context :fields idx :tincture)]
         [ui-interface/form-element (c/++ context :fields idx :pattern-scaling)]
         [ui-interface/form-element (c/++ context :fields idx :pattern-rotation)]
         [ui-interface/form-element (c/++ context :fields idx :pattern-offset-x)]
         [ui-interface/form-element (c/++ context :fields idx :pattern-offset-y)]])])])

(defn parent-context [{:keys [path] :as context}]
  (let [index (last path)
        parent-context (c/-- context 2)
        parent-type (interface/get-raw-data (c/++ parent-context :type))]
    (when (and (int? index)
               (-> parent-type (or :dummy) namespace (= "heraldry.field.type")))
      parent-context)))

(defn name-prefix-for-part [{:keys [path] :as context}]
  (when-let [parent-context (parent-context context)]
    (let [parent-type (interface/get-raw-data (c/++ parent-context :type))]
      (-> (field/part-name parent-type (last path))
          util/upper-case-first))))

(defn non-mandatory-part-of-parent? [{:keys [path] :as context}]
  (let [index (last path)]
    (when (int? index)
      (when-let [parent-context (parent-context context)]
        (>= index (field/mandatory-part-count parent-context))))))

(defmethod ui-interface/component-node-data :heraldry.component/field [{:keys [path] :as context}]
  (let [field-type (interface/get-raw-data (c/++ context :type))
        ref? (= field-type :heraldry.field.type/ref)
        tincture (interface/get-sanitized-data (c/++ context :tincture))
        components-context (c/++ context :components)
        num-components (interface/get-list-size components-context)]
    {:title (util/combine ": "
                          [(name-prefix-for-part context)
                           (if ref?
                             (str "like " (name-prefix-for-part
                                           (-> context
                                               c/--
                                               (c/++ (interface/get-raw-data
                                                      (c/++ context :index))))))
                             (field/title context))])
     :icon (case field-type
             :heraldry.field.type/plain (let [[scale-x
                                               scale-y
                                               translate-x
                                               translate-y] (if (= tincture :none)
                                                              [5 6 0 0]
                                                              [10 10 -15 -40])
                                              mask-id "preview-mask"
                                              icon [:svg {:version "1.1"
                                                          :xmlns "http://www.w3.org/2000/svg"
                                                          :xmlnsXlink "http://www.w3.org/1999/xlink"
                                                          :viewBox (str "0 0 120 140")
                                                          :preserveAspectRatio "xMidYMin slice"}
                                                    [:g {:transform "translate(10,10)"}
                                                     [:mask {:id mask-id}
                                                      [:rect {:x 0
                                                              :y 0
                                                              :width 100
                                                              :height 120
                                                              :stroke "none"
                                                              :fill "#fff"}]]
                                                     [:g {:mask (str "url(#" mask-id ")")}
                                                      [:g {:transform (str "translate(" translate-x "," translate-y ")")}
                                                       [:rect {:x 0
                                                               :y 0
                                                               :width 100
                                                               :height 120
                                                               :stroke "none"
                                                               :fill (tincture/pick tincture context)
                                                               :transform (str "scale(" scale-x "," scale-y ")")}]]]
                                                     [:rect {:x 0
                                                             :y 0
                                                             :width 100
                                                             :height 120
                                                             :stroke "#000"
                                                             :fill "none"}]]]]
                                          {:default icon
                                           :selected icon})
             :heraldry.field.type/ref {:default [:span {:style {:display "inline-block"}}]
                                       :selected [:span {:style {:display "inline-block"}}]}
             {:default (static/static-url
                        (str "/svg/field-type-" (name field-type) "-unselected.svg"))
              :selected (static/static-url
                         (str "/svg/field-type-" (name field-type) "-selected.svg"))})
     :validation (validation/validate-field context)
     :buttons (if ref?
                [{:icon "fas fa-sliders-h"
                  :title :string.user.button/change
                  :handler #(state/dispatch-on-event % [:override-field-part-reference path])}]
                (cond-> [{:icon "fas fa-plus"
                          :title :string.button/add
                          :menu [{:title :string.entity/ordinary
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/ordinary])}
                                 {:title :string.entity/charge
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/charge])}
                                 {:title :string.entity/charge-group
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/charge-group])}
                                 {:title :string.entity/semy
                                  :handler #(state/dispatch-on-event % [:add-element components-context default/semy])}]}
                         {:icon "fas fa-pen-nib"
                          :title :string.button/from-blazon
                          :handler #(state/dispatch-on-event % [:from-blazon context])}]
                  (non-mandatory-part-of-parent? context)
                  (conj {:icon "fas fa-undo"
                         :title "Reset"
                         :handler #(state/dispatch-on-event % [:reset-field-part-reference context])})))
     :nodes (concat (when (and (not (show-tinctures-only? field-type))
                               (-> field-type name keyword (not= :plain)))
                      (let [fields-context (c/++ context :fields)
                            num-fields (interface/get-list-size fields-context)]
                        (->> (range num-fields)
                             (map (fn [idx]
                                    {:context (c/++ fields-context idx)}))
                             vec)))
                    (->> (range num-components)
                         reverse
                         (map (fn [idx]
                                (let [component-context (c/++ components-context idx)]
                                  {:context component-context
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip :string.tooltip/move-down
                                              :handler #(state/dispatch-on-event % [:move-element component-context (dec idx)])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-components))
                                              :tooltip :string.tooltip/move-up
                                              :handler #(state/dispatch-on-event % [:move-element component-context (inc idx)])}
                                             {:icon "far fa-trash-alt"
                                              :remove? true
                                              :tooltip :string.tooltip/remove
                                              :handler #(state/dispatch-on-event % [:remove-element component-context])}]})))
                         vec))}))

(defmethod ui-interface/component-form-data :heraldry.component/field [_context]
  {:form form})
