(ns heraldicon.heraldry.ordinary.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn- shape-path [shape]
  (let [shape-path (:shape shape)]
    (cond->> shape-path
      (vector? shape-path) (apply str))))

(defn- shape-mask [context]
  (let [render-shape (interface/get-render-shape context)]
    [:path {:d (shape-path render-shape)
            :clip-rule "evenodd"
            :fill-rule "evenodd"
            :fill "#fff"}]))

(defn- render-line [context {:keys [line line-data line-from edge-paths]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))]
    (if edge-paths
      (when outline?
        (into [:g (outline/style context)]
              (map (fn [edge-path]
                     [:path {:d edge-path}]))
              edge-paths))
      [line/render line line-data line-from outline? context])))

(defn- edges [context]
  (let [{:keys [lines]} (interface/get-render-shape context)]
    (into [:g]
          (map (fn [line]
                 [render-line context line]))
          lines)))

(rf/reg-sub ::cottise?
  (fn [[_ {:keys [path]}] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [cottise? _]
    cottise?))

(declare render)

(defn- cottising [context]
  (let [cottising-context (c/++ context :cottising)
        cottise-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-1)])
        cottise-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-2)])
        cottise-opposite-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-opposite-1)])
        cottise-opposite-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-opposite-2)])]
    [:g
     (when cottise-1?
       [render (c/++ cottising-context :cottise-1)])
     (when cottise-opposite-1?
       [render (c/++ cottising-context :cottise-opposite-1)])

     (when (and cottise-1?
                cottise-2?)
       [render (c/++ cottising-context :cottise-2)])
     (when (and cottise-opposite-1?
                cottise-opposite-2?)
       [render (c/++ cottising-context :cottise-opposite-2)])]))

(defn render [{:keys [svg-export?]
               :as context}]
  (let [clip-path-id (uid/generate "clip")
        {:keys [transform]} (interface/get-properties context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [shape-mask context]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [:g (when transform
            {:transform transform})
       [field.shared/render (c/++ context :field)]]]
     [edges context]
     [cottising context]]))
