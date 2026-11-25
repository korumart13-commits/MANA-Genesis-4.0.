#!/usr/bin/env python3
"""Offline structural validator for the MANA Genesis glTF avatar asset."""
import base64
import json
import sys
from pathlib import Path

COMPONENT_BYTES = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}
TYPE_WIDTH = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def main(path):
    document = json.loads(Path(path).read_text(encoding="utf-8"))
    require(document.get("asset", {}).get("version") == "2.0", "glTF asset.version must be 2.0")

    buffers = document.get("buffers", [])
    require(bool(buffers), "at least one embedded buffer is required")
    decoded = []
    for index, buffer in enumerate(buffers):
        uri = buffer.get("uri", "")
        require(uri.startswith("data:application/octet-stream;base64,"), f"buffer {index} must be embedded base64")
        payload = base64.b64decode(uri.split(",", 1)[1], validate=True)
        require(len(payload) == buffer.get("byteLength"), f"buffer {index} byteLength mismatch")
        decoded.append(payload)

    views = document.get("bufferViews", [])
    for index, view in enumerate(views):
        buffer_index = view.get("buffer", 0)
        require(0 <= buffer_index < len(decoded), f"bufferView {index} has invalid buffer")
        offset = view.get("byteOffset", 0)
        length = view.get("byteLength", -1)
        require(offset >= 0 and length >= 0 and offset + length <= len(decoded[buffer_index]),
                f"bufferView {index} exceeds its buffer")

    accessors = document.get("accessors", [])
    for index, accessor in enumerate(accessors):
        view_index = accessor.get("bufferView")
        require(view_index is not None and 0 <= view_index < len(views), f"accessor {index} has invalid bufferView")
        component = accessor.get("componentType")
        width = TYPE_WIDTH.get(accessor.get("type"))
        require(component in COMPONENT_BYTES and width is not None, f"accessor {index} has invalid type")
        count = accessor.get("count", -1)
        require(count >= 0, f"accessor {index} has invalid count")
        view = views[view_index]
        stride = view.get("byteStride", COMPONENT_BYTES[component] * width)
        required = count * stride
        require(required <= view.get("byteLength", 0), f"accessor {index} exceeds its bufferView")

    materials = document.get("materials", [])
    meshes = document.get("meshes", [])
    require(bool(meshes), "avatar must contain meshes")
    for mesh_index, mesh in enumerate(meshes):
        for primitive in mesh.get("primitives", []):
            for semantic, accessor_index in primitive.get("attributes", {}).items():
                require(0 <= accessor_index < len(accessors), f"mesh {mesh_index} attribute {semantic} has invalid accessor")
            if "indices" in primitive:
                require(0 <= primitive["indices"] < len(accessors), f"mesh {mesh_index} has invalid index accessor")
            if "material" in primitive:
                require(0 <= primitive["material"] < len(materials), f"mesh {mesh_index} has invalid material")

    nodes = document.get("nodes", [])
    require(len(nodes) >= 30, "full-body avatar should contain at least 30 named parts")
    for index, node in enumerate(nodes):
        if "mesh" in node:
            require(0 <= node["mesh"] < len(meshes), f"node {index} has invalid mesh")
        for child in node.get("children", []):
            require(0 <= child < len(nodes), f"node {index} has invalid child")
    scenes = document.get("scenes", [])
    require(bool(scenes), "scene is missing")
    for scene in scenes:
        for node in scene.get("nodes", []):
            require(0 <= node < len(nodes), "scene references invalid node")

    # The character must have a connected articulated hierarchy, not only loose meshes.
    named_nodes = {node.get("name"): index for index, node in enumerate(nodes) if node.get("name")}
    joint_names = {
        "MANA_Shoulder_L", "MANA_Elbow_L", "MANA_Wrist_L",
        "MANA_Shoulder_R", "MANA_Elbow_R", "MANA_Wrist_R",
        "MANA_Hip_L", "MANA_Knee_L", "MANA_Ankle_L",
        "MANA_Hip_R", "MANA_Knee_R", "MANA_Ankle_R",
    }
    require(joint_names.issubset(named_nodes), "12 articulated joint nodes are required")
    root_index = named_nodes.get("MANA_Articulated_Root")
    require(root_index is not None, "articulated root node is missing")
    parents = {}
    for parent_index, node in enumerate(nodes):
        for child_index in node.get("children", []):
            require(child_index not in parents, f"node {child_index} has more than one parent")
            parents[child_index] = parent_index
    reachable = set()
    pending = [root_index]
    while pending:
        node_index = pending.pop()
        require(node_index not in reachable, "articulated hierarchy contains a cycle or duplicate child")
        reachable.add(node_index)
        pending.extend(nodes[node_index].get("children", []))
    require(len(reachable) == len(nodes), "all avatar parts must be reachable from the articulated root")

    animations = document.get("animations", [])
    require(bool(animations), "avatar must include at least one animation")
    animated_joint_names = set()
    for animation_index, animation in enumerate(animations):
        samplers = animation.get("samplers", [])
        channels = animation.get("channels", [])
        require(bool(samplers) and bool(channels), f"animation {animation_index} is empty")
        for sampler in samplers:
            require(0 <= sampler.get("input", -1) < len(accessors), "animation sampler has invalid input accessor")
            require(0 <= sampler.get("output", -1) < len(accessors), "animation sampler has invalid output accessor")
        for channel in channels:
            require(0 <= channel.get("sampler", -1) < len(samplers), "animation channel has invalid sampler")
            target = channel.get("target", {})
            require(0 <= target.get("node", -1) < len(nodes), "animation channel has invalid target node")
            require(target.get("path") in {"translation", "rotation", "scale", "weights"},
                    "animation channel has invalid target path")
            if target.get("node") in {named_nodes[name] for name in joint_names} and target.get("path") == "rotation":
                animated_joint_names.add(nodes[target["node"]].get("name"))

    require(len(animated_joint_names) >= 8, "idle animation must articulate at least eight arm/leg joints")

    print(
        f"glTF OK: {len(nodes)} nodes, {len(meshes)} meshes, {len(materials)} materials, "
        f"{len(animations)} animations, {sum(map(len, decoded))} embedded bytes"
    )


if __name__ == "__main__":
    try:
        if len(sys.argv) != 2:
            raise ValueError("usage: validate_mana_gltf.py <avatar.gltf>")
        main(sys.argv[1])
    except Exception as error:
        print(f"glTF validation failed: {error}", file=sys.stderr)
        sys.exit(1)
