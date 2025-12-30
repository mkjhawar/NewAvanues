import React from 'react';
import { TreeView as MuiTreeView, TreeItem } from '@mui/lab';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import FolderIcon from '@mui/icons-material/Folder';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import { Box, Typography } from '@mui/material';

export interface TreeNode {
  id: string;
  label: string;
  icon?: string;
  children?: TreeNode[];
}

export interface TreeViewProps {
  nodes: TreeNode[];
  expandedIds?: string[];
  onNodeClick?: (nodeId: string) => void;
  onToggle?: (nodeId: string) => void;
}

const renderTree = (
  nodes: TreeNode[],
  onNodeClick?: (nodeId: string) => void
): React.ReactNode => {
  return nodes.map(node => (
    <TreeItem
      key={node.id}
      nodeId={node.id}
      label={
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}>
          {node.children && node.children.length > 0 ? (
            <FolderIcon fontSize="small" color="action" />
          ) : (
            <InsertDriveFileIcon fontSize="small" color="action" />
          )}
          <Typography variant="body2">{node.label}</Typography>
        </Box>
      }
      onClick={() => onNodeClick?.(node.id)}
    >
      {node.children && renderTree(node.children, onNodeClick)}
    </TreeItem>
  ));
};

export const TreeView: React.FC<TreeViewProps> = ({
  nodes,
  expandedIds = [],
  onNodeClick,
  onToggle,
}) => {
  const [expanded, setExpanded] = React.useState<string[]>(expandedIds);

  const handleToggle = (_: React.SyntheticEvent, nodeIds: string[]) => {
    setExpanded(nodeIds);
    const diff = nodeIds.filter(id => !expanded.includes(id));
    diff.forEach(id => onToggle?.(id));
  };

  return (
    <MuiTreeView
      defaultCollapseIcon={<ExpandMoreIcon />}
      defaultExpandIcon={<ChevronRightIcon />}
      expanded={expanded}
      onNodeToggle={handleToggle}
      sx={{ flexGrow: 1 }}
    >
      {renderTree(nodes, onNodeClick)}
    </MuiTreeView>
  );
};

export default TreeView;
